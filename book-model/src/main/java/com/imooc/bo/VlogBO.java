package com.imooc.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class VlogBO {


    private String id;
    @NotBlank(message = "视频作者不能为空")
    private String vlogerId;
    @NotBlank(message = "视频地址不能为空")
    @URL(message = "视频地址不合法")//校验是否是合法的url
    private String url;
    @NotBlank(message = "视频封面不能为空")
    private String cover;
    @NotBlank(message = "视频标题不能为空")
    private String title;
    @NotNull(message = "视频宽度不能为空")
    private Integer width;
    @NotNull(message = "视频高度不能为空")
    private Integer height;
    private Integer likeCounts;
    private Integer commentsCounts;
}
