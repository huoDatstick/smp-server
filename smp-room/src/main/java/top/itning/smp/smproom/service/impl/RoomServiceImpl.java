package top.itning.smp.smproom.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import top.itning.smp.smproom.client.InfoClient;
import top.itning.smp.smproom.config.CustomProperties;
import top.itning.smp.smproom.dao.StudentRoomCheckDao;
import top.itning.smp.smproom.entity.StudentRoomCheck;
import top.itning.smp.smproom.entity.User;
import top.itning.smp.smproom.exception.GpsException;
import top.itning.smp.smproom.exception.IllegalCheckException;
import top.itning.smp.smproom.exception.SavedException;
import top.itning.smp.smproom.exception.UserNameDoesNotExistException;
import top.itning.smp.smproom.security.LoginUser;
import top.itning.smp.smproom.service.RoomService;
import top.itning.utils.tuple.Tuple2;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author itning
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RoomServiceImpl implements RoomService {
    private static final Logger logger = LoggerFactory.getLogger(RoomServiceImpl.class);
    private final StudentRoomCheckDao studentRoomCheckDao;
    private final InfoClient infoClient;
    private final CustomProperties customProperties;

    @Autowired
    public RoomServiceImpl(StudentRoomCheckDao studentRoomCheckDao, InfoClient infoClient, CustomProperties customProperties) {
        this.studentRoomCheckDao = studentRoomCheckDao;
        this.infoClient = infoClient;
        this.customProperties = customProperties;
    }

    @Override
    public Page<StudentRoomCheck> getRoomCheckInfoByStudentUserName(String username, Pageable pageable) {
        User user = infoClient.getUserInfoByUserName(username).orElseThrow(() -> new UserNameDoesNotExistException("用户名不存在", HttpStatus.NOT_FOUND));
        return studentRoomCheckDao.findAllByUser(user, pageable);
    }

    @Override
    public StudentRoomCheck check(MultipartFile file, LoginUser loginUser, double longitude, double latitude) throws IOException {
        if (longitude > 180.0D || longitude < -180.0D || latitude > 90.0D || latitude < -90.0D) {
            throw new GpsException(longitude, latitude);
        }
        User user = infoClient.getUserInfoByUserName(loginUser.getUsername()).orElseThrow(() -> new UserNameDoesNotExistException("用户名不存在", HttpStatus.NOT_FOUND));
        Tuple2<Date, Date> dateRange = getDateRange(new Date());
        if (studentRoomCheckDao.existsByUserAndCheckTimeBetween(user, dateRange.getT1(), dateRange.getT2())) {
            throw new IllegalCheckException("您今天已经打过卡了，不能重复打卡");
        }
        StudentRoomCheck studentRoomCheck = new StudentRoomCheck();
        studentRoomCheck.setUser(user);
        studentRoomCheck.setLongitude(longitude);
        studentRoomCheck.setLatitude(latitude);
        studentRoomCheck.setCheckTime(new Date());
        StudentRoomCheck saved = studentRoomCheckDao.save(studentRoomCheck);
        String filenameExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (filenameExtension == null) {
            logger.warn("use default extension for path {}", file.getOriginalFilename());
            filenameExtension = "jpg";
        }
        if (!StringUtils.hasText(saved.getId())) {
            throw new SavedException("数据库存储ID为空", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        file.transferTo(new File(customProperties.getResourceLocation() + saved.getId() + "." + filenameExtension));
        return saved;
    }

    @Override
    public List<StudentRoomCheck> checkAll(Date whereDay) {
        Tuple2<Date, Date> dateRange = getDateRange(whereDay);
        return studentRoomCheckDao.findAllByCheckTimeBetweenOrderByCheckTimeDesc(dateRange.getT1(), dateRange.getT2());
    }

    private Tuple2<Date, Date> getDateRange(Date startDate) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(startDate);
        cal1.set(Calendar.HOUR_OF_DAY, 0);
        cal1.set(Calendar.MINUTE, 0);
        cal1.set(Calendar.SECOND, 0);
        cal1.set(Calendar.MILLISECOND, 0);
        Date date1 = cal1.getTime();

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date1);
        cal2.add(Calendar.DAY_OF_YEAR, 1);
        return new Tuple2<>(date1, cal2.getTime());
    }
}
