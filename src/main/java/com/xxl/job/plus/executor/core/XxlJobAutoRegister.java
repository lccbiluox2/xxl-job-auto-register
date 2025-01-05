package com.xxl.job.plus.executor.core;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.plus.executor.annotation.XxlRegister;
import com.xxl.job.plus.executor.entity.AddOrUpdateTaskInfo;
import com.xxl.job.plus.executor.entity.UpdateResult;
import com.xxl.job.plus.executor.model.XxlJobGroup;
import com.xxl.job.plus.executor.model.XxlJobInfo;
import com.xxl.job.plus.executor.service.JobGroupService;
import com.xxl.job.plus.executor.service.JobInfoService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * XxlJobAutoRegister 是一个组件类，负责在应用启动后自动注册执行器和任务到 xxl-job 管理平台。
 */

public abstract class XxlJobAutoRegister implements ApplicationListener<ApplicationReadyEvent>, ApplicationContextAware {

    private static final Log log = LogFactory.get();

    private ApplicationContext applicationContext;

    @Autowired
    private JobGroupService jobGroupService;

    @Autowired
    private JobInfoService jobInfoService;

    /**
     * 设置 Spring 应用上下文，以便于后续使用。
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 在应用程序完全启动并准备好处理请求时触发此方法，用于初始化操作。
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("收到集群启动成功事件..");
        log.info("准备注册执行器..");

        // 注册执行器
        addJobGroup();
        log.info("执行器注册完成.");

        log.info("准备同步任务..");
        XxlJobGroup xxlJobGroup = getXxlJobGroup();
        if (xxlJobGroup == null) {
            log.error("未能获取有效的执行器组，无法继续任务同步.");
            return;
        }

        List<XxlJobInfo> allRemoteTask = getAllRemoteTask(xxlJobGroup);
        List<XxlJobInfo> allLocalTask = getAllLocalTask(xxlJobGroup);

        AddOrUpdateTaskInfo addOrUpdateTaskInfo = getAddOrUpdateTask(allRemoteTask, allLocalTask);

        // 添加新的任务
        addJobInfo(addOrUpdateTaskInfo.getAddTask());

        // 更新已存在的任务
        updateJobInfo(addOrUpdateTaskInfo.getUpdateTask());

        // 将数据写入到本地数据库
        addOrUpdateTaskToDb(addOrUpdateTaskInfo);
        log.info("所有任务同步完成.");
    }

    /**
     * 比较本地和远程任务列表，返回需要添加和更新的任务信息。
     */
    private AddOrUpdateTaskInfo getAddOrUpdateTask(List<XxlJobInfo> allRemoteTask, List<XxlJobInfo> allLocalTask) {
        // 创建一个以 executorHandler 为键的任务映射，方便快速查找
        Map<String, XxlJobInfo> remoteTaskMap = allRemoteTask.stream()
                .collect(Collectors.toMap(XxlJobInfo::getExecutorHandler, job -> job, (existing, replacement) -> existing));

        List<XxlJobInfo> addTask = new ArrayList<>();
        List<XxlJobInfo> updateTask = new ArrayList<>();

        for (XxlJobInfo local : allLocalTask) {
            String handler = local.getExecutorHandler();
            if (!remoteTaskMap.containsKey(handler)) {
                // 如果远程任务中没有找到对应的本地任务，则添加到新增列表
                addTask.add(local);
            } else {
                // 如果存在，则比较其他字段是否相同，不同则加入更新列表
                XxlJobInfo remote = remoteTaskMap.get(handler);
                UpdateResult updateResult = checkAndUpdate(local, remote);
                if (updateResult.isNeedsUpdate()) {
                    // 如果需要更新，需要把本地变动的信息 和远程的id 联系在一起
                    XxlJobInfo remoteUpdate =createUpdateTaskInfo(local,remote);
                    updateTask.add(remoteUpdate);
                }
            }
        }

        return new AddOrUpdateTaskInfo(addTask, updateTask);
    }

    private XxlJobInfo createUpdateTaskInfo(XxlJobInfo local, XxlJobInfo remote) {
        local.setId(remote.getId());
        return local;
    }

    /**
     * 判断两个任务信息是否需要更新，即检查所有相关字段是否有变化。
     */
    /**
     * 判断两个任务信息是否需要更新，并记录发生变化的字段及变化前后的信息。
     */
    private UpdateResult checkAndUpdate(XxlJobInfo local, XxlJobInfo remote) {
        UpdateResult result = new UpdateResult();

        // Helper method to compare fields and record changes with old and new values
        BiConsumer<String, Supplier<String>> compareAndRecord = (fieldName, supplier) -> {
            String localValue = supplier.get();
            String remoteValue = reflectGet(remote, fieldName);
            if (!Objects.equals(localValue, remoteValue)) {
                result.setNeedsUpdate(true);
                result.addChangedField(fieldName, remoteValue, localValue);
            }
        };

        // 比较所有相关字段
        compareAndRecord.accept("jobDesc", () -> local.getJobDesc());
        compareAndRecord.accept("author", () -> local.getAuthor());
        compareAndRecord.accept("alarmEmail", () -> local.getAlarmEmail());
        compareAndRecord.accept("scheduleType", () -> local.getScheduleType());
        compareAndRecord.accept("scheduleConf", () -> local.getScheduleConf());
        compareAndRecord.accept("misfireStrategy", () -> local.getMisfireStrategy());
        compareAndRecord.accept("executorRouteStrategy", () -> local.getExecutorRouteStrategy());
        compareAndRecord.accept("executorParam", () -> local.getExecutorParam());
        compareAndRecord.accept("executorBlockStrategy", () -> local.getExecutorBlockStrategy());
        compareAndRecord.accept("glueType", () -> local.getGlueType());
        compareAndRecord.accept("glueSource", () -> local.getGlueSource());
        compareAndRecord.accept("glueRemark", () -> local.getGlueRemark());
        compareAndRecord.accept("childJobId", () -> local.getChildJobId());

        // 对于基本类型字段，直接比较值并记录变化前后的信息
        if (local.getExecutorTimeout() != remote.getExecutorTimeout()) {
            result.setNeedsUpdate(true);
            result.addChangedField("executorTimeout", String.valueOf(remote.getExecutorTimeout()), String.valueOf(local.getExecutorTimeout()));
        }
        if (local.getExecutorFailRetryCount() != remote.getExecutorFailRetryCount()) {
            result.setNeedsUpdate(true);
            result.addChangedField("executorFailRetryCount", String.valueOf(remote.getExecutorFailRetryCount()), String.valueOf(local.getExecutorFailRetryCount()));
        }
        if (local.getTriggerStatus() != remote.getTriggerStatus()) {
            result.setNeedsUpdate(true);
            result.addChangedField("triggerStatus", String.valueOf(remote.getTriggerStatus()), String.valueOf(local.getTriggerStatus()));
        }

        return result;
    }

    // 辅助方法：通过反射获取远程对象的字段值（仅用于字符串类型）
    private String reflectGet(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            throw new RuntimeException("Reflective access failed.", e);
        }
    }

    /**
     * 将需要添加或更新的任务信息写入到本地数据库。
     */
    public void addOrUpdateTaskToDb(AddOrUpdateTaskInfo addOrUpdateTaskInfo) {

    }

    private void updateJobInfo(List<XxlJobInfo> updateTask) {
        for (XxlJobInfo item : updateTask) {
            try {
                Integer jobId = jobInfoService.updateTask(item);
                log.info("成功更新任务，任务ID: {}", jobId);
            } catch (Exception e) {
                log.error("更新任务过程中发生异常: ", e);
            }
        }
    }

    private void addJobInfo(List<XxlJobInfo> addTask) {
        for (XxlJobInfo item : addTask) {
            try {
                Integer jobId = jobInfoService.addJobInfo(item);
                log.info("成功添加新任务，任务ID: {}", jobId);
            } catch (Exception e) {
                log.error("添加任务过程中发生异常: ", e);
            }
        }
    }

    private List<XxlJobInfo> getAllRemoteTask(XxlJobGroup xxlJobGroup) {
        try {
            return jobInfoService.getJobInfo(xxlJobGroup.getId(), null);
        } catch (Exception e) {
            log.error("获取远程任务列表失败: ", e);
            throw new RuntimeException("获取远程任务列表失败", e);
        }
    }

    private XxlJobGroup getXxlJobGroup() {
        try {
            List<XxlJobGroup> jobGroups = jobGroupService.getJobGroup();
            if (jobGroups.isEmpty()) {
                log.error("未找到任何执行器组，无法继续.");
                return null;
            }

            XxlJobGroup xxlJobGroup = jobGroups.get(0);
            log.info("选择执行器组: {}", xxlJobGroup);
            return xxlJobGroup;
        } catch (Exception e) {
            log.error("查询执行器异常: ", e);
            throw new RuntimeException("查询执行器异常", e);
        }
    }

    private List<XxlJobInfo> getAllLocalTask(XxlJobGroup xxlJobGroup) {
        try {
            List<XxlJobInfo> result = new ArrayList<>();

            String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);
            for (String beanDefinitionName : beanDefinitionNames) {
                Object bean = applicationContext.getBean(beanDefinitionName);
                log.debug("检查 Bean: {}", beanDefinitionName);

                Map<Method, XxlJob> annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                        (Method method) -> AnnotatedElementUtils.findMergedAnnotation(method, XxlJob.class));

                for (Map.Entry<Method, XxlJob> entry : annotatedMethods.entrySet()) {
                    Method executeMethod = entry.getKey();
                    XxlJob xxlJob = entry.getValue();

                    if (executeMethod.isAnnotationPresent(XxlRegister.class)) {
                        XxlRegister xxlRegister = executeMethod.getAnnotation(XxlRegister.class);

                        // 创建并注册新的任务信息
                        XxlJobInfo newJobInfo = createXxlJobInfo(xxlJobGroup, xxlJob, xxlRegister);
                        result.add(newJobInfo);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("任务注册过程中发生异常: ", e);
            throw new RuntimeException("任务注册失败", e);
        }
    }

    /**
     * 自动注册执行器到 xxl-job 管理平台。
     */
    private void addJobGroup() {
        if (jobGroupService.preciselyCheck()) {
            log.info("执行器已经存在，无需再次注册.");
            return;
        }

        if (jobGroupService.autoRegisterGroup()) {
            log.info("自动注册 xxl-job 执行器成功！");
        } else {
            log.error("自动注册 xxl-job 执行器失败！");
        }
    }

    /**
     * 创建一个新的任务信息对象。
     *
     * @param xxlJobGroup 执行器组信息
     * @param xxlJob      任务注解信息
     * @param xxlRegister 任务注册注解信息
     * @return 新创建的任务信息对象
     */
    private XxlJobInfo createXxlJobInfo(XxlJobGroup xxlJobGroup, XxlJob xxlJob, XxlRegister xxlRegister) {
        XxlJobInfo xxlJobInfo = new XxlJobInfo();
        xxlJobInfo.setJobGroup(xxlJobGroup.getId());
        xxlJobInfo.setJobDesc(xxlRegister.jobDesc());
        xxlJobInfo.setAuthor(xxlRegister.author());
        xxlJobInfo.setEditAble(xxlRegister.editAble());
        xxlJobInfo.setScheduleType("CRON");
        xxlJobInfo.setScheduleConf(xxlRegister.cron());
        xxlJobInfo.setGlueType("BEAN");
        xxlJobInfo.setExecutorHandler(xxlJob.value());
        xxlJobInfo.setExecutorRouteStrategy(xxlRegister.executorRouteStrategy());
        xxlJobInfo.setMisfireStrategy("DO_NOTHING");
        xxlJobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        xxlJobInfo.setExecutorTimeout(0);
        xxlJobInfo.setExecutorFailRetryCount(0);
        xxlJobInfo.setGlueRemark("GLUE代码初始化");
        xxlJobInfo.setTriggerStatus(xxlRegister.triggerStatus());

        return xxlJobInfo;
    }
}