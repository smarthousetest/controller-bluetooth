package com.github.smarthousetest.bluetooth;

import org.apache.tomcat.jni.Local;
import org.springframework.stereotype.Service;

import javax.bluetooth.*;
import javax.bluetooth.UUID;
import java.io.IOException;
import java.util.*;

@Service
public class BluetoothService {

    private final Object inquiryCompletedEvent = new Object();
    private final Object lock = new Object();

    private final LocalDevice localDevice;

    public BluetoothService() throws BluetoothStateException{
        this.localDevice = LocalDevice.getLocalDevice();
    }

    private Map<String, RemoteDevice> devices = new HashMap<>();

    private DiscoveryListener listener = new DiscoveryListener() {

        public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
            System.out.println("Device " + btDevice.getBluetoothAddress() + " found");
            devices.put(btDevice.getBluetoothAddress(), btDevice);
            try {
                System.out.println("     name " + btDevice.getFriendlyName(false));
            } catch (IOException cantGetDeviceName) {
            }
        }

        public void inquiryCompleted(int discType) {
            System.out.println("Device Inquiry completed!");
            synchronized (inquiryCompletedEvent) {
                inquiryCompletedEvent.notifyAll();
            }
        }

        @Override
        public void serviceSearchCompleted(int arg0, int arg1) {
            synchronized (lock) {
                lock.notify();
            }
        }

        @Override
        public void servicesDiscovered(int arg0, ServiceRecord[] services) {
            System.out.println(services.length);
            for (int i = 0; i < services.length; i++) {
                String url = services[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                if (url == null) {
                    continue;
                }

                DataElement serviceName = services[i].getAttributeValue(0x0100);
                if (serviceName != null) {
                    System.out.println("service " + serviceName.getValue() + " found " + url);
                } else {
                    System.out.println("service found " + url);
                }

                if(serviceName.getValue().equals("OBEX Object Push")){
                    System.out.println("Obex url: " + url);
                }
            }

        }
    };




    public Map<String, RemoteDevice> findDevices(int accessCode)
            throws BluetoothStateException, InterruptedException{


        synchronized (inquiryCompletedEvent) {
            boolean started = localDevice.getDiscoveryAgent().
                    startInquiry(accessCode, listener);
            if (started) {
                System.out.println("wait for device inquiry to complete...");
                inquiryCompletedEvent.wait();
                System.out.println(devices.size() + " device(s) found");
                return devices;
            } else {
                throw new RuntimeException("started failed");
            }
        }
    }

    public void test(String macAddress) throws BluetoothStateException{
        List<UUID> list = new ArrayList<>();

        list.add(new UUID(0x1105));//OBEX Object Push service
        list.add(new UUID(0x110A));
        list.add(new UUID(0x110B));
        list.add(new UUID(0x110C));
        list.add(new UUID(0x110D));
        list.add(new UUID(0x110E));
        list.add(new UUID(0x110F));

        UUID[] uuidSet = list.toArray(UUID[]::new);

        int[] attrIDs =  new int[] {
                0x0100 // Service name
        };
        
        localDevice.getDiscoveryAgent().
                searchServices(attrIDs, uuidSet, this.devices.get(macAddress), listener);
        try {
            synchronized(lock){
                lock.wait();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
    }


}
