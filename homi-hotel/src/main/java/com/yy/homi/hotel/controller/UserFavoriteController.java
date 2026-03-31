package com.yy.homi.hotel.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.service.UserFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

@Validated
@RestController
@RequestMapping("/userfavorite")
public class UserFavoriteController {

    @Autowired
    private UserFavoriteService favoriteService;

    @GetMapping("/toggle")
    public R toggle(@RequestParam("userId") @NotBlank(message = "userId不能为空！") String userId,
                    @RequestParam("hotelId") @NotBlank(message = "hotelId不能为空！") String hotelId) {
        return favoriteService.toggleFavorite(userId, hotelId);
    }

    @GetMapping("/list")
    public R list(@RequestParam("userId") @NotBlank(message = "userId不能为空！") String userId,
                  @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                  @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return favoriteService.getMyFavoriteList(userId, pageNum, pageSize);
    }

    @GetMapping("/checkFavoriteStatus")
    public R status(@RequestParam("userId") @NotBlank(message = "userId不能为空！") String userId,
                    @RequestParam("hotelId") @NotBlank(message = "hotelId不能为空！") String hotelId) {
        return favoriteService.checkFavoriteStatus(userId, hotelId);
    }

    @GetMapping("/getFavorites")
    public R getViewHistory(@Validated @RequestParam("userId") @NotBlank(message = "userId不能为空！") String userId,
                            @RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,
                            @RequestParam(value = "pageSize",defaultValue = "10") Integer pageSize){
        return favoriteService.getFavorites(userId,pageNum,pageSize);
    }

    @GetMapping("/countFavoriteByUserId")
    public R countFavoriteByUserId(@RequestParam("userId") @NotBlank(message = "用户id不能为空！") String userId){
        return favoriteService.countFavoriteByUserId(userId);
    }

}