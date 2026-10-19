"""External HTTP black-box tests and a small local latency sample, using a dedicated test server."""
import concurrent.futures, datetime, json, os, pathlib, statistics, time, unittest, urllib.error, urllib.request, uuid

BASE=os.environ.get('DELIVERY_TEST_API','http://127.0.0.1:8088/api/v1')
OUT=pathlib.Path('delivery/evidence'); OUT.mkdir(parents=True,exist_ok=True)
def request(method,path,body=None,user=None):
    headers={'Content-Type':'application/json'}
    if user is not None: headers['X-User-Id']=str(user)
    req=urllib.request.Request(BASE+path, data=None if body is None else json.dumps(body,ensure_ascii=False).encode(),method=method,headers=headers)
    try:
        with urllib.request.urlopen(req,timeout=65) as response: code=response.status; raw=response.read()
    except urllib.error.HTTPError as error: code=error.code;raw=error.read()
    value=json.loads(raw)
    if isinstance(value,dict) and value.get('code')==0 and 'data' in value:value=value['data']
    return code,value

class BlackBox(unittest.TestCase):
    def setUp(self):
        code,data=request('POST','/users',{'username':'bb-'+uuid.uuid4().hex,'passwordHash':'local-test-only'})
        self.assertEqual(code,200,data);self.user=data['id']
    def snapshot(self):return request('GET','/schedule',user=self.user)[1]
    def command(self,type,payload,revision=None,key=None):
        return request('POST','/workspace/commands',{'operationKey':key or str(uuid.uuid4()),'baseRevision':self.snapshot()['planningRevision'] if revision is None else revision,'authorizationScope':'schedule:write:self','type':type,'payload':payload},self.user)
    def payload(self):
        deadline=(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=3)).replace(microsecond=0).isoformat()
        return {'title':'黑盒任务','deadline':deadline,'durationMinutes':60,'priority':2,'courseId':''}
    def create(self):
        code,r=self.command('CREATE_TASK',self.payload());self.assertEqual(code,200,r);return self.snapshot()['tasks'][0]['id']
    def test01_health_and_beijing_snapshot(self):
        code,s=request('GET','/schedule',user=self.user);self.assertEqual(code,200);self.assertEqual(s['timezone'],'Asia/Shanghai')
    def test02_missing_identity(self):self.assertEqual(request('GET','/schedule')[0],401)
    def test03_create_idempotent(self):
        key=str(uuid.uuid4());p=self.payload();self.assertEqual(self.command('CREATE_TASK',p,0,key)[0],200);self.assertEqual(self.command('CREATE_TASK',p,0,key)[0],200);self.assertEqual(len(self.snapshot()['tasks']),1)
    def test04_bound_key_changed_payload(self):
        key=str(uuid.uuid4());p=self.payload();self.command('CREATE_TASK',p,0,key);p['title']='changed';self.assertEqual(self.command('CREATE_TASK',p,0,key)[0],409)
    def test05_stale_revision(self):self.create();self.assertEqual(self.command('CREATE_TASK',self.payload(),0)[0],409)
    def test06_invalid_duration_and_priority(self):
        for field,value in [('durationMinutes',0),('priority',5)]:
            p=self.payload();p[field]=value;self.assertEqual(self.command('CREATE_TASK',p)[0],400)
        self.assertEqual(len(self.snapshot()['tasks']),0)
    def test07_past_deadline_rollback(self):
        p=self.payload();p['deadline']='2020-01-01T00:00:00Z';self.assertGreaterEqual(self.command('CREATE_TASK',p)[0],400);self.assertEqual(self.snapshot()['planningRevision'],0)
    def test08_preview_confirm_undo(self):
        self.create();code,p=request('POST','/planning/previews',{},self.user);self.assertEqual(code,200,p);self.assertTrue(p['candidate']['blocks']);self.assertEqual(self.snapshot()['tasks'][0]['scheduledBlocks'],[])
        code,r=self.command('APPLY_PLAN',{'previewId':p['previewId']});self.assertEqual(code,200,r);self.assertTrue(self.snapshot()['tasks'][0]['scheduledBlocks'])
        self.assertEqual(self.command('UNDO',{'executionId':r['executionId']})[0],200);self.assertEqual(self.snapshot()['tasks'][0]['scheduledBlocks'],[])
    def test09_completion_and_profile(self):
        id=self.create();code,r=self.command('COMPLETE_TASK',{'taskId':id,'actualMinutes':90});self.assertEqual(code,200,r);self.assertFalse(r['undoable']);self.assertEqual(self.snapshot()['tasks'][0]['status'],'DONE');self.assertTrue(request('GET','/profile',user=self.user)[1]['history'])
    def test10_other_user_task_rejected(self):
        self.create();other=request('POST','/users',{'username':'bb-'+uuid.uuid4().hex,'passwordHash':'local-test-only'})[1]['id'];code,_=request('POST','/workspace/commands',{'operationKey':str(uuid.uuid4()),'baseRevision':0,'type':'CANCEL_TASK','authorizationScope':'schedule:write:self','payload':{'taskId':self.snapshot()['tasks'][0]['id']}},other);self.assertIn(code,[403,404])
    def test11_legacy_writes_blocked(self):self.assertEqual(request('POST','/tasks',{},self.user)[0],409)
    def test12_profile_persists_and_invalidates_revision(self):
        code,r=request('POST','/profile/preferences',{'key':'planning.dailyLimitMinutes','value':180,'action':'SET','baseRevision':0},self.user);self.assertEqual(code,200,r);self.assertEqual(self.snapshot()['activeFacts'][0]['value'],180)
        self.assertEqual(request('POST','/profile/preferences',{'key':'planning.dailyLimitMinutes','value':300,'action':'SET','baseRevision':0},self.user)[0],409)
    def test13_model_clarification_and_failure_not_success(self):
        for text,expected in [('缺少信息','NEEDS_CLARIFICATION'),('失败','FAILED')]:
            code,r=request('POST','/ai/previews',{'instruction':text},self.user);self.assertEqual(code,200,r);self.assertEqual(r['status'],expected)
        self.assertEqual(len(self.snapshot()['tasks']),0)
    def test14_concurrent_same_revision_one_winner(self):
        payload=self.payload()
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool: codes=list(pool.map(lambda _:self.command('CREATE_TASK',payload,0)[0],range(2)))
        self.assertEqual(sorted(codes),[200,409]);self.assertEqual(len(self.snapshot()['tasks']),1)
    def test15_course_and_fixed_window(self):
        start=(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=1)).replace(hour=2,minute=0,second=0,microsecond=0)
        body={'name':'黑盒课程','kind':'COURSE','start':start.isoformat(),'end':(start+datetime.timedelta(hours=1)).isoformat(),'baseRevision':0}
        code,r=request('POST','/workspace/fixed',body,self.user);self.assertEqual(code,200,r);self.assertEqual(len(self.snapshot()['courses']),1)

class EvidenceResult(unittest.TextTestResult):
    def __init__(self,*a,**kw):super().__init__(*a,**kw);self.cases=[]
    def startTest(self,test):self.started=time.perf_counter();super().startTest(test)
    def addSuccess(self,test):self.cases.append({'case':test.id().split('.')[-1],'status':'PASS','milliseconds':round((time.perf_counter()-self.started)*1000,2)});super().addSuccess(test)
    def addFailure(self,test,err):self.cases.append({'case':test.id(),'status':'FAIL','error':str(err[1])});super().addFailure(test,err)
    def addError(self,test,err):self.cases.append({'case':test.id(),'status':'ERROR','error':str(err[1])});super().addError(test,err)

if __name__=='__main__':
    result=unittest.TextTestRunner(verbosity=2,resultclass=EvidenceResult).run(unittest.defaultTestLoader.loadTestsFromTestCase(BlackBox))
    evidence={'executedAt':datetime.datetime.now().isoformat(),'server':BASE,'aiProvider':'deterministic local HTTP fixture','testsRun':result.testsRun,'successful':result.wasSuccessful(),'cases':result.cases}
    (OUT/'blackbox.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2),encoding='utf8')
    if result.wasSuccessful():
        def sample(_):
            start=time.perf_counter();code,_=request('GET','/schedule',user=1);return (time.perf_counter()-start)*1000,code
        sequential=[sample(i) for i in range(50)]
        start=time.perf_counter()
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:parallel=list(pool.map(sample,range(80)))
        duration=time.perf_counter()-start
        stats={'note':'本机专用测试服务的小样本延迟与8并发冒烟，不代表生产容量或长期压力测试','sequentialRequests':50,'sequentialP50Ms':round(statistics.median([v[0] for v in sequential]),2),'sequentialP95Ms':round(sorted(v[0] for v in sequential)[47],2),'parallelRequests':80,'workers':8,'parallelSeconds':round(duration,3),'allHttp200':all(v[1]==200 for v in sequential+parallel)}
        (OUT/'performance.json').write_text(json.dumps(stats,ensure_ascii=False,indent=2),encoding='utf8');print(json.dumps(stats,ensure_ascii=False))
    raise SystemExit(0 if result.wasSuccessful() else 1)
