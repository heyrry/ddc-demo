import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author baofeng
 * @date 2022/08/29
 */
@ComponentScan(basePackages = {"beans, beans.dao"})
public class TestConfiguration {

}
