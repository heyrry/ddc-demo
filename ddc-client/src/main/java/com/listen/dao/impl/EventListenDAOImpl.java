package com.listen.dao.impl;

import com.listen.dao.EventListenDAO;
import com.listen.dto.EventListenDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    private static final String UPDATE_ERROR_INFO_SQL = "update ddc_event_listen set gmt_modified=now(),error_info=LEFT(CONCAT(IFNULL(error_info,''),?),2000) where id=?";
    private static final String QUERY_PENDING_SQL = "select * from ddc_event_listen where listen_result > 0 and gmt_modified < date_sub(now(), interval ? second) limit ?";
    private static final String INCREMENT_RETRY_SQL = "update ddc_event_listen set retry_times=retry_times+1,gmt_modified=now() where id=?";

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
        if (id == null || StringUtils.isEmpty(errorInfo)) {
            return false;
        }
        // SQL 已通过 CONCAT(IFNULL(error_info,''), ?) 追加，并截断至 2000 字符
        // 单次追加内容限制 500 字符，防止单条堆栈撑满字段
        String truncated = StringUtils.abbreviate(errorInfo, 500);
        try {
            int result = jdbcTemplate.update(UPDATE_ERROR_INFO_SQL, "，" + truncated, id);
            return result > 0;
        } catch (Exception e) {
            log.error("updateErrorInfo error, id:{}, errorInfo:{}", id, errorInfo, e);
        }
        return false;
    }

    @Override
    public List<EventListenDO> queryPendingEventListenList(int delaySeconds, int limit) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate.query(QUERY_PENDING_SQL, rowMapper, delaySeconds, limit);
    }

    @Override
    public void incrementRetryTimes(Long id) {
        jdbcTemplate.update(INCREMENT_RETRY_SQL, id);
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

}
