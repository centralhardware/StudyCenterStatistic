package me.centralhardware.znatoki.telegram.statistic;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.mapper.TeacherNameMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ReportService {

    private final TimeMapper timeMapper;
    private final TeacherNameMapper teacherNameMapper;

    public List<File> getReportsCurrent(){
        return getReport(timeMapper::getCuurentMontTimes);
    }

    public List<File> getReportPrevious(){
        return getReport(timeMapper::getPrevMonthTimes);
    }

    private List<File> getReport(Function<Long,List<Time>> getTime){
        return timeMapper.getIds()
                .stream()
                .map(getTime)
                .filter(CollectionUtils::isNotEmpty)
                .map(it -> new MonthReport().generate(it, teacherNameMapper::getFio))
                .toList();
    }

}
