package com.imooc.service;

import com.imooc.base.BaseInfoProperties;
import com.imooc.mapper.VlogMapper;
import com.imooc.pojo.Vlog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FlushTaskService extends BaseInfoProperties {
    @Autowired
    private VlogMapper vlogMapper;

    @Autowired
    private VlogService vlogService;

    @Value("${nacos.counts}")
    private Integer nacosCounts;

    //定时任务，每天的凌晨3点执行，开启多线程刷新数据到数据库里面
    @Scheduled(cron = "0 0 3 * * ?")
    public void flushTask() {

        Integer counts = 0;
        List<Vlog> list = vlogMapper.selectAll();
        for( Vlog vlog : list ) {
            String countsStr = redis.get(REDIS_VLOG_BE_LIKED_COUNTS + ":" + vlog.getId());
            if(StringUtils.isNotBlank(countsStr)){
                counts = Integer.valueOf(countsStr);
                if(counts >= nacosCounts){
                    vlogService.flushCounts(vlog.getId(),Integer.valueOf(countsStr));
                }
            }
        }

    }
}
