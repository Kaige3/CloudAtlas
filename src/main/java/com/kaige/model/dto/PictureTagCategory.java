package com.kaige.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureTagCategory implements Serializable {

    private List<String> tags;
    private List<String> categories;

}
