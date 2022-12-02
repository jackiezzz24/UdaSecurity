package com.udacity.security.service;

import com.udacity.image.service.ImageService;
import com.udacity.security.application.StatusListener;
import com.udacity.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestSecurityService {
    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private Set<StatusListener> statusListeners = new HashSet<>();
    private SecurityService securityService;
    private Sensor sensorDoor;
    private Sensor sensorWindow;
    private Sensor sensorMotion;
    private Set<Sensor> sensors;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);

        sensors = new HashSet<>();
        sensorDoor = new Sensor("sensorDoor", SensorType.DOOR);
        sensorWindow = new Sensor("sensorWindow", SensorType.WINDOW);
        sensorMotion = new Sensor("sensorMotion", SensorType.MOTION);
        sensors.add(sensorDoor);
        sensors.add(sensorWindow);
        sensors.add(sensorMotion);
    }

    @Test
    void alarmAndSensorActivated_setPending() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(sensorDoor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void alarmAndSensorActivatedAndPending_setAlarm() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(sensorDoor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void sensorInactivatedAndPending_setNoAlarm() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensorDoor.setActive(true);
        sensorWindow.setActive(true);
        sensorMotion.setActive(true);
        securityService.changeSensorActivationStatus(sensorDoor, false);
        securityService.changeSensorActivationStatus(sensorWindow, false);
        securityService.changeSensorActivationStatus(sensorMotion, false);

        verify(securityRepository, times(3)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void alarmActivated_NotAffactAlarm(boolean status) {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensorDoor, status);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void sensorActivatedWhileActiveAndPending_setAlarm() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensorDoor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    void sensorInactivatedWhileInactive_NoChange(AlarmStatus status) {
        when(securityService.getAlarmStatus()).thenReturn(status);
        sensorDoor.setActive(false);
        securityService.changeSensorActivationStatus(sensorDoor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void containsCatWhileArmed_setAlarm() {
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void notContainCatAndSensorInactive_setNoAlarm() {
        when(securityService.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void systemDisarmed_setNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void systemArmed_setSensorInactive() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        statusListeners.forEach(sl -> sl.sensorStatusChanged());
    }

    @Test
    void systemArmedWithCat_setAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
}
