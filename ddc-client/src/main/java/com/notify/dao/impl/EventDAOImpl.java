package com.notify.dao.impl;

import com.notify.dao.AbstractEventDAO;
import com.notify.dao.EventDAO;
import com.notify.dto.EventDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

/**
 * 通过jdbc更新
 * @author baofeng
 * @date 2023/07/04
 */
@Slf4j
public class EventDAOImpl extends AbstractEventDAO {

    private static final String UPDATE_EVENT_BY_NOTIFY_SQL = "update ddc_event set gmt_modified=now(),state=?,notify_type=?,notify_id=?,notify_result=?,version=version+1 where id=? and version=?";
    private static final String QUERY_PENDING_SQL = "select * from ddc_event where state in ('pending','failed') and gmt_event < TIMESTAMPADD(SECOND, -?, NOW()) limit ?";

    private DataSource dataSource;
    protected Connection connection;

    private static final BeanPropertyRowMapper<EventDO> ROW_MAPPER = new BeanPropertyRowMapper<>(EventDO.class);


    @Override
    public void updateEventNotify(EventDO event) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Object[] params = new Object[] {
                event.getState(),
                event.getNotifyType(),
                event.getNotifyId(),
                event.getNotifyResult(),
                event.getId(),
                event.getVersion()
        };
        int result = jdbcTemplate.update(UPDATE_EVENT_BY_NOTIFY_SQL, params);
        log.info("updateEventNotify end, event:{}, result:{}", event, result);
    }

    @Override
    public List<EventDO> queryPendingEventList(int delaySeconds, int limit) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate.query(QUERY_PENDING_SQL, ROW_MAPPER, delaySeconds, limit);
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

}
