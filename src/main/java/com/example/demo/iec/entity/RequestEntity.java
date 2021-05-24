package com.example.demo.iec.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestEntity {
    String objectName;
    String fc;
    Object objectValue;
}
