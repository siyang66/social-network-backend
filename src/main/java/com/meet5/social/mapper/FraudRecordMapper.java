package com.meet5.social.mapper;

import com.meet5.social.model.FraudRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FraudRecordMapper {

    void insert(FraudRecord record);
}
