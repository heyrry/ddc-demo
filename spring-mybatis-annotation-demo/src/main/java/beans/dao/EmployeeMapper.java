package beans.dao;

import model.Employee;
import org.apache.ibatis.annotations.*;

/**
 * @author baofeng
 * @date 2022/08/30
 */
@Mapper
public interface EmployeeMapper {

    /**
     * 查询
     * @param id
     * @return
     */
    @Select("select id, name, state, age from a where id = #{id}")
    Employee getEmpById(@Param("id") Integer id);

    /**
     * 插入
     * @param employee
     * @return
     */
    @Insert("insert into a(name) value(#{employee.name})")
    int insert(@Param("employee") Employee employee);

    /**
     * 更新
     * @param employee
     * @return
     */
    @Update("update a set a.name = #{employee.name} where a.id = #{employee.id}")
    int update(@Param("employee") Employee employee);
}
