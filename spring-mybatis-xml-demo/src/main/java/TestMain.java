import beans.FooServiceImpl;
import model.Employee;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author baofeng
 * @date 2022/03/14
 */
public class TestMain {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        FooServiceImpl fooService = context.getBean(FooServiceImpl.class);
//        fooService.noInsert(new Employee(1, "a", "b", "c"));
        fooService.update(new Employee(1, "a", "b", "c"));
//        fooService.update2(new Employee(1, "a", "b", "c"));
    }
}
