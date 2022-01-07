package com.thiendz.tool.fplautocms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.ToString;

@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckAppDto extends ResponseDto<ToolInfoDto> {
}
