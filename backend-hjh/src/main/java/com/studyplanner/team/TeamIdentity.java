package com.studyplanner.team;
import com.studyplanner.common.*;
public final class TeamIdentity {
 private TeamIdentity(){}
 public static Long user(){Long id=CurrentUserContext.getUserId();if(id==null)throw new BusinessException(401,"请选择有效的本机用户");return id;}
}
