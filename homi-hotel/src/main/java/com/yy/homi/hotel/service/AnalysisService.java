package com.yy.homi.hotel.service;

import com.yy.homi.common.domain.entity.R;

public interface AnalysisService {
    R getHotelStarDistribute();


    R getCityHotelCount();

    R getOpenYearDistribute();

    R getCommentScoreDistribute();

    R getHotelRadarStats();

    R getCommentCountDistribute();

    R getRoomPriceDistribute();

    R getBedTypeDistribute();

    R getRoomAreaDistribute();

    R getRoomBubbleStats();

    R getRoomFacilityWordCloud();

    R getFacilityTypeDistribute();

    R getHotelFacilityWordCloud();

    R getSurroundingDistanceDistribute();

    R getStarSurroundingAvg();

    R getUserActiveTimeDistribution();

    R getUserStarPreference();

    R getTopBrowsedCities();

    R getUserPricePreference();

    R getUserActionFunnel();
}
