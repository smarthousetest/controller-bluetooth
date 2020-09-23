package com.github.smarthousetest;

import com.github.smarthousetest.bluetooth.BluetoothService;
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.bluetooth.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class Controller {

    @Autowired
    private BluetoothService bluetoothService;

    @GetMapping("/devices")
    public Object devices(@RequestParam("type")String type) throws Exception {
        Map<String, RemoteDevice> devices = bluetoothService.
                findDevices(DiscoveryAgent.class.getField(type).getInt(null));

        return devices.values().stream().map(d -> {
            try {
                return d.getBluetoothAddress()+":"+d.getFriendlyName(false);
            } catch (IOException e){
                return d.getBluetoothAddress();
            }
        }).collect(Collectors.joining("\n"));
    }

    @GetMapping("/services")
    public Object services() throws BluetoothStateException{
        this.bluetoothService.test("D8CE3A120786");
        return "nice";
    }

}
