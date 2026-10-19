package com.studyplanner.importing;

import java.nio.*;
import java.nio.charset.*;
import java.time.Instant;
import java.util.*;
import com.studyplanner.agent.ModelAdapter;
import com.studyplanner.business.ScheduleStore;
import com.studyplanner.contract.*;

/** Real UTF-8 text interpretation; unsupported media remain explicit manual-review proposals. */
public final class TextImportModelAdapter implements ImportModelPort {
    private final ModelAdapter model;
    private final ScheduleStore store;
    private final boolean configured;
    private final MockImportModelAdapter manual=new MockImportModelAdapter();
    public TextImportModelAdapter(ModelAdapter model,ScheduleStore store,boolean configured) {
        this.model=model;this.store=store;this.configured=configured;
    }
    @Override public ImportProposal propose(ImportSource source) {
        if(source.sourceType()!=ImportSourceType.TEXT || !configured)return manual.propose(source);
        final String text;
        try {text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(source.content())).toString().strip();}
        catch(CharacterCodingException e) {throw new IllegalArgumentException("文本文件必须为 UTF-8 编码");}
        if(text.isBlank() || text.length()>1300)throw new IllegalArgumentException("文本需包含 1 至 1300 字");
        var response=model.generate("请从以下原始文本提取一个待校对的学习任务，仅建议 CREATE_TASK，不安排现有任务；不完整的信息放入 ambiguities。原始文本：\n"+text,store.snapshot());
        var ambiguities=new ArrayList<>(response.ambiguities());
        var changes=response.changes();
        String title=changes.getOrDefault("title",com.fasterxml.jackson.databind.node.TextNode.valueOf("文本导入待核对")).asText("文本导入待核对");
        if(title.isBlank() || title.length()>200) {title="文本导入待核对";ambiguities.add("请填写任务名称");}
        Instant deadline=null;
        try {if(changes.containsKey("deadline"))deadline=Instant.parse(changes.get("deadline").asText());}
        catch(java.time.DateTimeException e) {ambiguities.add("请核对截止时间格式");}
        if(deadline==null)ambiguities.add("请补充截止时间");
        Integer duration=null;var durationNode=changes.get("durationMinutes");
        if(durationNode!=null && durationNode.isIntegralNumber() && durationNode.canConvertToInt()
                && durationNode.asInt()>0 && durationNode.asInt()<=10080)duration=durationNode.asInt();
        if(duration==null)ambiguities.add("请补充预计耗时");
        if(response.type()!=ActionType.CREATE_TASK)ambiguities.add("请将来源整理为一个任务并人工核对");
        return new ImportProposal(1,"proposal-"+source.requestKey(),"ASSIGNMENT",title,null,deadline,duration,
                List.of(source.fileName()),List.copyOf(new LinkedHashSet<>(ambiguities)),ImportStatus.REVIEW,List.of());
    }
}
