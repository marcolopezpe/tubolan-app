package pe.marcolopez.apps.tubolan;

import io.quarkiverse.fx.FxApplication;
import io.quarkiverse.fx.FxApplicationStartupEvent;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;
import pe.marcolopez.apps.tubolan.networks.DeviceNetwork;
import pe.marcolopez.apps.tubolan.runneables.FileReceiverServer;
import pe.marcolopez.apps.tubolan.runneables.LanDiscoveryBroadcaster;
import pe.marcolopez.apps.tubolan.runneables.LanDiscoveryListener;
import pe.marcolopez.apps.tubolan.config.DeviceSession;

@Slf4j
@QuarkusMain
public class QuarkusFxApplication implements QuarkusApplication {

  @Inject
  DeviceSession sessionData;

  @Inject
  DeviceNetwork deviceNetwork;

  @Inject
  LanDiscoveryListener lanDiscoveryListener;

  @Inject
  FileReceiverServer fileReceiverServer;

  @Inject
  LanDiscoveryBroadcaster lanDiscoveryBroadcaster;

  @Override
  public int run(String... args) throws Exception {
    Application.launch(FxApplication.class, args);
    return 0;
  }

  void onApplicationStartup(@Observes FxApplicationStartupEvent event) {
    log.info("###########################");
    log.info("### Application started ###");
    log.info("###########################");

    log.info("### Device Name: {}", deviceNetwork.getDeviceLocal().getName());
    log.info("### Device IP: {}", deviceNetwork.getDeviceLocal().getIp());

    log.info("### Setting up session data...");
    sessionData.setDevice(deviceNetwork.getDeviceLocal());
    sessionData.setDarkMode(false);

    log.info("### Starting LAN discovery listener...");
    lanDiscoveryListener.setIp(sessionData.getDevice().getIp());
    lanDiscoveryListener.start();

    log.info("### Starting file receiver server...");
    fileReceiverServer.startServer();

    log.info("### Starting LAN discovery broadcaster...");
    lanDiscoveryBroadcaster.setDeviceIp(sessionData.getDevice().getIp());
    lanDiscoveryBroadcaster.setDeviceName(sessionData.getDevice().getName());
    lanDiscoveryBroadcaster.start();

    log.info("### Application started successfully.");
  }
}