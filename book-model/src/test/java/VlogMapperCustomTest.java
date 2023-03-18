//import com.imooc.MyApplication;
//import com.imooc.mapper.VlogMapperCustom;
//import com.imooc.vo.IndexVlogVO;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringRunner;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@SpringBootTest
//@ContextConfiguration(classes = MyApplication.class)
//@RunWith(SpringRunner.class)
//public class VlogMapperCustomTest {
//
//    @Autowired
//    private VlogMapperCustom vlogMapperCustom;
//
//    @Test
//    public void getIndexVlogList() {
//        Map<String, Object> map = new HashMap<>();
//        map.put("search","测试");
//        List<IndexVlogVO> indexVlogList = vlogMapperCustom.getIndexVlogList(map);
//        for (IndexVlogVO indexVlogVO : indexVlogList) {
//            System.out.println(indexVlogVO);
//        }
//    }
//
//    @Test
//    public void getVlogDetailById() {
//        Map<String, Object> map = new HashMap<>();
//        map.put("vlogId","1");
//        List<IndexVlogVO> indexVlogList = vlogMapperCustom.getVlogDetailById(map);
//        for (IndexVlogVO indexVlogVO : indexVlogList) {
//            System.out.println(indexVlogVO);
//        }
//    }
//
//    @Test
//    public void getMyLikedVlogList() {
//        Map<String, Object> map = new HashMap<>();
//        map.put("userId","1");
//        List<IndexVlogVO> indexVlogList = vlogMapperCustom.getMyLikedVlogList(map);
//        for (IndexVlogVO indexVlogVO : indexVlogList) {
//            System.out.println(indexVlogVO);
//        }
//    }
//
//    @Test
//    public void getMyFollowVlogList() {
//        Map<String, Object> map = new HashMap<>();
//        map.put("userId","1");
//        List<IndexVlogVO> indexVlogList = vlogMapperCustom.getMyFollowVlogList(map);
//        for (IndexVlogVO indexVlogVO : indexVlogList) {
//            System.out.println(indexVlogVO);
//        }
//    }
//
//    @Test
//    public void getMyFriendVlogList() {
//        Map<String, Object> map = new HashMap<>();
//        map.put("userId","1");
//        List<IndexVlogVO> indexVlogList = vlogMapperCustom.getMyFriendVlogList(map);
//        for (IndexVlogVO indexVlogVO : indexVlogList) {
//            System.out.println(indexVlogVO);
//        }
//    }
//}
//
//
