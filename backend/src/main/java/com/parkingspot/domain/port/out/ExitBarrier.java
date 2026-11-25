package com.parkingspot.domain.port.out;

public interface ExitBarrier {
    void open(long siteId, String plate);
}
