package org.ovirt.engine.core.bll.validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.ovirt.engine.core.common.BackendService;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.utils.ansible.AnsibleConstants;
import org.ovirt.engine.core.utils.threadpool.ThreadPools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AnsibleRunnerCleanUpService implements BackendService {

    private static Logger log = LoggerFactory.getLogger(AnsibleRunnerCleanUpService.class);

    @Inject
    @ThreadPools(ThreadPools.ThreadPoolType.EngineScheduledThreadPool)
    private ManagedScheduledExecutorService executor;

    @PostConstruct
    public void scheduleJob() {
        double interval = Config.<Double> getValue(ConfigValues.AnsibleRunnerArtifactsCleanupCheckTimeInHours);
        final int HOURS_TO_MINUTES = 60;
        long intervalInMinutes = Math.round(interval * HOURS_TO_MINUTES);

        executor.schedule(this::checkExecutionTimeStamp,
                intervalInMinutes,
                TimeUnit.MINUTES);
    }

    private void checkExecutionTimeStamp() {
        int artifactsLifeTime = Config.<Integer> getValue(ConfigValues.AnsibleRunnerArtifactsLifetimeInDays);
        Stream.of(new File(AnsibleConstants.ANSIBLE_RUNNER_PATH.toString()).listFiles()).forEach(file -> {
            long creationInDays;
            try {
                creationInDays = Files.readAttributes(file.toPath(), BasicFileAttributes.class)
                        .creationTime()
                        .to(TimeUnit.DAYS);
            } catch (IOException e) {
                log.error("Failed to read file '{}' attributes: {}", file.getAbsolutePath(), e.getMessage());
                log.debug("Exception: ", e);
                return;
            }
            long todayInDays = FileTime.fromMillis(new Date().getTime()).to(TimeUnit.DAYS);
            if (todayInDays - creationInDays > artifactsLifeTime) {
                try {
                    Files.walk(Paths.get(file.getAbsolutePath()))
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    log.error("Failed to delete dir '{}' content: {}", file.getAbsolutePath(), e.getMessage());
                    log.debug("Exception: ", e);
                }
            }
        });
    }
}
