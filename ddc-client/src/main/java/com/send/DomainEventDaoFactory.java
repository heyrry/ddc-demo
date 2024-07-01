package com.send;

import com.listen.dao.impl.EventListenDAOImpl;
import com.notify.dao.EventDAO;
import com.notify.dao.impl.EventDAOImpl;
import com.notify.dao.impl.InsertEventDAOImpl;
import com.notify.dto.EventDO;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author baofeng
 * @date 2023/07/04
 */
public class DomainEventDaoFactory {

    public static EventDAO createInsertEventDAO(Connection connection) {
        InsertEventDAOImpl eventDAO = new InsertEventDAOImpl();
        eventDAO.setConnection(connection);
        return eventDAO;
    }

    public static EventDAO createEventDAO(DataSource dataSource) {
        EventDAOImpl eventDAO = new EventDAOImpl();
        eventDAO.setDataSource(dataSource);
        return eventDAO;
    }

    public static EventListenDAOImpl createEventListenDAO(DataSource dataSource) {
        EventListenDAOImpl eventDAO = new EventListenDAOImpl();
        eventDAO.setDataSource(dataSource);
        return eventDAO;
    }


}
