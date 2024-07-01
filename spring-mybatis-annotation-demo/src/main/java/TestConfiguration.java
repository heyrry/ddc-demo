import com.config.ClientConfiguration;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * @author baofeng
 * @date 2022/08/29
 */
@ComponentScan(basePackages = {"beans"})
@EnableAspectJAutoProxy
@Import(ClientConfiguration.class)
public class TestConfiguration {

}
