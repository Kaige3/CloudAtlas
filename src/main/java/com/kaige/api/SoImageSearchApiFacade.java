package com.kaige.api;

import com.kaige.api.model.SoImageSearchDto;
import com.kaige.api.sub.GetSoImageListApi;
import com.kaige.api.sub.GetSoImageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class SoImageSearchApiFacade {

    public static List<SoImageSearchDto> searchImage(String imageUrl, Integer start) {
        String soImageUrl = GetSoImageUrlApi.getSoImageUrl(imageUrl);
        return GetSoImageListApi.getImageList(soImageUrl, start);
    }

    public static void main(String[] args) {
        //test
        String url = "https://p2.ssl.qhimgs1.com/bdr/468_250_/t0293cacd5a5178709e.jpg";
        List<SoImageSearchDto> imageList = searchImage(url, 0);
        System.out.println(imageList);
    }
}
