package beans.dao;

import model.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author baofeng
 * @date 2022/08/30
 */
//@Mapper
public interface EmployeeNoAnnotationMapper {

    /**
     * 查询
     * @param id
     * @return
     */
    Employee getEmpById(@Param("id") Integer id);

    /**
     * 插入
     * @param employee
     * @return
     */
    int insert(@Param("employee") Employee employee);

    /**
     * 更新
     * @param employee
     * @return
     */
    int update(@Param("employee") Employee employee);
}
