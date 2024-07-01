package com.notify.dao.impl;

import com.notify.dao.AbstractEventDAO;
import com.notify.dto.EventDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 通过与业务共用connect更新（同一事务）
 * @author baofeng
 * @date 2023/07/04
 */
@Slf4j
public class InsertEventDAOImpl extends AbstractEventDAO {

    private static final String INSERT_SQL = "insert into ddc_event(gmt_create,gmt_modified,domain,entity_id,event,gmt_event,event_context,state,notify_type,gmt_notify,notify_id,notify_result,retry_times,version,local_send,remote_send) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private Connection connection;


    @Override
    public Long saveEvent(EventDO event) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = connection.prepareStatement(INSERT_SQL, PreparedStatement.RETURN_GENERATED_KEYS);
            Object[] params = parseInsertParams(event);
            ArgumentPreparedStatementSetter argumentSetter = new ArgumentPreparedStatementSetter(params);
            argumentSetter.setValues(ps);
            int result = ps.executeUpdate();
            if (result <= 0) {
                return null;
            }
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                long id = rs.getLong(1);
                if (id > 0) {
                    log.info("saveEvent end, event:{}, id:{}", event, id);
                    event.setId(id);
                    return id;
                }
            }
            throw new IllegalStateException("saveEvent sqlExecuteIllegal,sql:" + INSERT_SQL);
        } catch (SQLException e) {
            throw new SQLStateSQLExceptionTranslator().translate("saveEvent", INSERT_SQL, e);
        } finally {
            JdbcUtils.closeResultSet(rs);
            JdbcUtils.closeStatement(ps);
        }
    }

    private Object[] parseInsertParams(EventDO event) {
        return new Object[] {event.getGmtEvent(), event.getGmtModified(), event.getDomain(), event.getEntityId(), event.getEvent(), event.getGmtEvent(), event.getEventContext(), event.getState(), event.getNotifyType(), event.getGmtNotify(),
                event.getNotifyId(), event.getNotifyResult(), event.getRetryTimes(), event.getVersion(), event.getLocalSend(), event.getRemoteSend()};
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

}
