package com.listen.dao.impl;

import com.listen.dao.EventListenDAO;
import com.listen.dto.EventListenDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @author baofeng
 * @date 2023/07/04
 */
@Slf4j
public class EventListenDAOImpl implements EventListenDAO {

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    private static final String SELECT_RESULT_SQL = "select * from ddc_event_listen where event_id = ?";
    private static final String INSERT_RESULT_SQL = "insert into ddc_event_listen(gmt_create,gmt_modified,event_id,domain,event,event_content,msg_id,listen_names,listen_result,error_info,retry_times) values(?,?,?,?,?,?,?,?,?,?,?)";
    private static final String UPDATE_RESULT_SQL = "update ddc_event_listen set gmt_modified=now(),listen_result=listen_result-? where id=? and mod(listen_result,?)>=?";
    private static final String UPDATE_ERROR_INFO_SQL = "update ddc_event_listen set gmt_modified=now(),error_info=? where id=?";

    /**
     * 创建一个BeanPropertyRowMapper实例，用于属性映射
     */
    BeanPropertyRowMapper<EventListenDO> rowMapper = new BeanPropertyRowMapper<>(EventListenDO.class);

    @Override
    public EventListenDO queryEventListen(Long eventId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<EventListenDO> list = jdbcTemplate.query(SELECT_RESULT_SQL, rowMapper, eventId);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new IllegalStateException("multiple eventListen,eventId:" + eventId);
        }
        return list.get(0);
    }

    @Override
    public Long saveEventListen(EventListenDO eventListen) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Object[] params = parseInsertParams(eventListen);
        int result = jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
                ArgumentPreparedStatementSetter argumentSetter = new ArgumentPreparedStatementSetter(params);
                PreparedStatement ps = conn.prepareStatement(INSERT_RESULT_SQL, PreparedStatement.RETURN_GENERATED_KEYS);
                argumentSetter.setValues(ps);
                return ps;
            }
        }, keyHolder);
        if (result <= 0) {
            return null;
        }
        Long id = keyHolder.getKey().longValue();
        eventListen.setId(id);
        return id;
    }

    private Object[] parseInsertParams(EventListenDO eventListen) {
        return new Object[] {eventListen.getGmtCreate(), eventListen.getGmtModified(), eventListen.getEventId(),
                eventListen.getDomain(), eventListen.getEvent(), eventListen.getListenContent(),
                eventListen.getTraceId(), eventListen.getListenNames(), eventListen.getListenResult(),
                eventListen.getErrorInfo(), eventListen.getRetryTimes()};
    }

    @Override
    public boolean updateEventListenResult(Long id, int index) {
        if (id == null || index < 0) {
            return false;
        }
        long value = Math.round(Math.pow(2, index));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        int result = jdbcTemplate.update(UPDATE_RESULT_SQL, new Object[] {value, id, value * 2, value});
        return result > 0;
    }

    @Override
    public boolean updateErrorInfo(Long id, String errorInfo) {
        if (StringUtils.isEmpty(errorInfo)) {
            return false;
        }
        try {
            EventListenDO eventListen = this.queryEventListen(id);
            if (eventListen == null) {
                return false;
            }
            // 错误信息进行拼装
            String newErrorInfo = composeErrorInfo(eventListen.getErrorInfo(), errorInfo);
            int result = jdbcTemplate.update(UPDATE_ERROR_INFO_SQL, new Object[] {newErrorInfo, id});
            return result > 0;
        } catch (Exception e) {
            log.error("DomainEventListenServiceImpl updateErrorInfo error, id:{}, message:{}, error:{}", id.toString(), errorInfo, ExceptionUtils.getStackTrace(e));
        }
        return false;
    }

    private String composeErrorInfo (String oldErrorInfo, String errorInfo) {
        // TODO 防止字符串过大，需要精简
        return oldErrorInfo + "，" + errorInfo;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

}
