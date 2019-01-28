package com.adobe.aam.shredder.ec2.service

import com.adobe.aam.shredder.ec2.aws.LifecycleHandler
import com.adobe.aam.shredder.ec2.log.ShredderLogUploader
import com.adobe.aam.shredder.ec2.notifier.Notifier
import com.adobe.aam.shredder.ec2.runner.BlockOnShutdownFailure
import com.adobe.aam.shredder.ec2.runner.ShutdownCommandsRunner
import com.adobe.aam.shredder.ec2.trigger.ShutdownLifecycleHookMessage
import com.adobe.aam.shredder.ec2.trigger.ShutdownTriggerListener
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.BlockingVariable

class ShutdownServiceTest extends Specification {
    @Unroll("test shutdown service. expectedShutDownSuccessful=#expectedShutDownSuccessful")
    def "test shutdown service"(shutdownOnStartupFail, shutDownScriptsResult, startupSuccessful,
                                expectedShutDownSuccessful, expectedLogsUploaded, expectedMonitoringSystemNotified) {
        given:

        def monitoringSystemWasNotified = new BlockingVariable<Boolean>()
        def logsWereUploaded = new BlockingVariable<Boolean>()

        def shutdownLifecycleHookMessage = Mock(ShutdownLifecycleHookMessage) {
            getEc2InstanceId() >> "11111";
            getLifecycleHookName() >> "LCHN";
            getAutoScalingGroupName() >> "ASGN";
        }

        def shutdownTriggerListener = Mock(ShutdownTriggerListener) {
            listenForShutdownTrigger(_) >> shutdownLifecycleHookMessage;
        }

        def notifier = Mock(Notifier) {
            notifyMonitoringServiceAboutShutdown(_) >> {
                monitoringSystemWasNotified.set(true)
            }
        }

        def shutDownCommandsRunner = Mock(ShutdownCommandsRunner) {
            getRunShutdownScriptsResult(_) >> shutDownScriptsResult
        }

        def shredderLogUploader = Mock(ShredderLogUploader) {
            uploadShutdownLogs(_) >> {
                logsWereUploaded.set(true)
            }
        }

        def lifecycleHandler = Mock(LifecycleHandler) {
            successfulCompleteLifecycle(_) >> null
        }

        def blockOnShutdownFailure = Mock(BlockOnShutdownFailure) {
            blockOnShutdownFailure(_) >> null
        }

        def shutdownService = new ShutdownService(shutdownTriggerListener, notifier, shutDownCommandsRunner,
                shredderLogUploader, blockOnShutdownFailure, shutdownOnStartupFail, lifecycleHandler)

        when:
        def shutDownSuccessful = shutdownService.getShutdownResult(startupSuccessful);

        then:
        shutDownSuccessful == expectedShutDownSuccessful

        and:
        logsWereUploaded.get() == expectedLogsUploaded

        and:
        monitoringSystemWasNotified.get() == expectedMonitoringSystemNotified

        where:
        shutdownOnStartupFail | shutDownScriptsResult | startupSuccessful || expectedShutDownSuccessful | expectedMonitoringSystemNotified | expectedLogsUploaded
        true                  | true                  | true              || true                       | true                             | true
        true                  | true                  | false             || true                       | true                             | true
        false                 | false                 | true              || false                      | true                             | true
        false                 | false                 | false             || false                      | true                             | true
    }
}
