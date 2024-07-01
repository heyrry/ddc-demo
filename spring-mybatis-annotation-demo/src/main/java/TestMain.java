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
//        fooService.noInsert();
//        fooService.noInsert2();
//        fooService.noInsert3();
//        fooService.noInsert4();
        fooService.update();
//        fooService.update2();
//        fooService.update3();
//        fooService.update4();
    }
}
