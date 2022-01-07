package com.thiendz.tool.fplautocms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class KeyValueDto {
    String key;
    String value;

    public static KeyValueDto map(String str) {
        KeyValueDto keyValueDto = new KeyValueDto();
        String[] spl = str.split("=");
        keyValueDto.setKey(spl[0]);
        if (spl.length > 1)
            keyValueDto.setValue(spl[1]);
        return keyValueDto;
    }
}
