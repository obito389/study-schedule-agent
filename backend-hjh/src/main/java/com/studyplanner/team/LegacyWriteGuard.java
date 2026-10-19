package com.studyplanner.team;
import java.util.Set;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.studyplanner.common.BusinessException;
@Component public class LegacyWriteGuard implements HandlerInterceptor {
 private final Set<String> roots=Set.of("tasks","task-steps","time-blocks","agent-actions","plan-drafts","profile-facts","profile-events","execution-logs","learning-stats","import-jobs","user-preferences","availability-windows","courses","course-sessions","semesters");
 @Override public boolean preHandle(HttpServletRequest req,HttpServletResponse res,Object handler){
  if(Set.of("POST","PUT","PATCH","DELETE").contains(req.getMethod())){
   String path=req.getRequestURI();if(path.startsWith("/api/v1/")){String root=path.substring(8).split("/")[0];if(roots.contains(root))throw new BusinessException(409,"旧CRUD写接口已停用，请使用工作台或统一执行器，避免绕过版本与约束");}
  }return true;
 }
}
