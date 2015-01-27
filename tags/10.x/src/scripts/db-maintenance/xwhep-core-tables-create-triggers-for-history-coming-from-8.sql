-- ===========================================================================
--
--  Copyright 2013-2014  E. URBAH
--                       at LAL, Univ Paris-Sud, IN2P3/CNRS, Orsay, France
--  License GPL v3
--
--  XtremWeb-HEP Core Tables :
--  SQL script creating, inside an 9.x schema, triggers permitting to populate
--  automatically the new history tables and columns with data coming from
--  an 8.x schema or from the 8.x software.
--
-- ===========================================================================
delimiter //


-- ---------------------------------------------------------------------------
select 'Table "users_history"' as '';
-- Sync "userRightId" with "rights"
-- ---------------------------------------------------------------------------
create trigger trig_users_history_insert_rights before insert on users_history
for each row
begin
  if   new.rights is not null
  then
    set @userRightId =
           ( select userRights.userRightId
             from   userRights
             where  userRights.userRightName = new.rights );
    if (select @userRightId) is not null
    then set new.userRightId = @userRightId;
    end if;
  end if;
end;//

create trigger trig_users_history_update_rights before update on users_history
for each row
begin
  if   (new.rights is null) and (old.rights is not null)
  then set new.userRightId = null;
  end if;
  
  if   (  new.rights is not null  )  and
       ( (old.rights is     null) or (old.rights <> new.rights) )
  then
    set @userRightId =
           ( select userRights.userRightId
             from   userRights
             where  userRights.userRightName = new.rights );
    if (select @userRightId) is not null
    then set new.userRightId = @userRightId;
    end if;
  end if;
end;//


-- ---------------------------------------------------------------------------
select 'Table "hosts_history"' as '';
-- Sync "osId"       with "os"
-- Sync Column "osId"               with "os"
--             "cpuTypeId"          with "cputype"
--             "usergroupUID"       with "project"
--      Table  "sharedAppTypes"     with "sharedapps"
--             "sharedPackageTypes" with "sharedpackages"
-- ---------------------------------------------------------------------------
create trigger trig_hosts_history_insert_os_cpu_project before insert on hosts_history
for each row
begin
  if   new.os is not null
  then
    set @osId =
           ( select oses.osId
             from   oses
             where  oses.osName = new.os );
    if (select @osId) is not null
    then set new.osId = @osId;
    end if;
  end if;
  
  if   new.cputype is not null
  then
    set @cpuTypeId =
           ( select cpuTypes.cpuTypeId
             from   cpuTypes
             where  cpuTypes.cpuTypeName = new.cputype );
    if (select @cpuTypeId) is not null
    then set new.cpuTypeId = @cpuTypeId;
    end if;
  end if;
  
  if   new.project is not null
  then
    set @usergroupUID =
           ( select usergroups.uid
             from   usergroups
             where  usergroups.label = new.project );
    if (select @usergroupUID) is not null
    then set new.usergroupUID = @usergroupUID;
    end if;
  end if;
end;//


create trigger trig_hosts_history_update_os_cpu_project before update on hosts_history
for each row
begin
  if   (new.os is null) and (old.os is not null)
  then set new.osId = null;
  end if;
  
  if   (  new.os is not null  )  and
       ( (old.os is     null) or (old.os <> new.os) )
  then
    set @osId =
           ( select oses.osId
             from   oses
             where  oses.osName = new.os );
    if (select @osId) is not null
    then set new.osId = @osId;
    end if;
  end if;
  
  if   (new.cputype is null) and (old.cputype is not null)
  then set new.cpuTypeId = null;
  end if;
  
  if   (  new.cputype is not null  )  and
       ( (old.cputype is     null) or (old.cputype <> new.cputype) )
  then
    set @cpuTypeId =
           ( select cpuTypes.cpuTypeId
             from   cpuTypes
             where  cpuTypes.cpuTypeName = new.cputype );
    if (select @cpuTypeId) is not null
    then set new.cpuTypeId = @cpuTypeId;
    end if;
  end if;
  
  if   (new.project is null) and (old.project is not null)
  then set new.usergroupUID = null;
  end if;
  
  if   (  new.project is not null  )  and
       ( (old.project is     null) or (old.project <> new.project) )
  then
    set @usergroupUID =
           ( select usergroups.uid
             from   usergroups
             where  usergroups.label = new.project );
    if (select @usergroupUID) is not null
    then set new.usergroupUID = @usergroupUID;
    end if;
  end if;
end;//


create trigger trig_hosts_history_insert_shared after insert on hosts_history
for each row
begin
  insert into sharedAppTypes_history ( hostUID, appTypeId )
  select new.uid, appTypes.appTypeId
  from   appTypes
  where  (new.sharedApps is not null) and (new.sharedApps <> '') and
         locate(appTypes.appTypeName, new.sharedApps);
  
  insert into sharedPackageTypes_history ( hostUID, packageTypeId )
  select new.uid, packageTypes.packageTypeId
  from   packageTypes
  where  (new.sharedApps is not null) and (new.sharedApps <> '') and
         locate(packageTypes.packageTypeName, new.sharedApps);
end;//


create trigger trig_hosts_history_update_shared after update on hosts_history
for each row
begin
  if   (  old.sharedapps is not null  ) and
       ( (new.sharedapps is     null) or (new.sharedapps <> old.sharedapps) )
  then delete from  sharedAppTypes_history
              where sharedAppTypes_history.hostUID = old.uid;
  end if;
  
  if   (  new.sharedapps is not null  )  and
       ( (old.sharedapps is     null) or (old.sharedapps <> new.sharedapps) )
  then insert into  sharedAppTypes_history ( hostUID, appTypeId )
       select new.uid, appTypes.appTypeId
       from   appTypes
       where  (new.sharedApps is not null) and (new.sharedApps <> '') and
              locate(appTypes.appTypeName, new.sharedApps);
  end if;
  
  if   (  old.sharedapps is not null  ) and
       ( (new.sharedapps is     null) or (new.sharedapps <> old.sharedapps) )
  then delete from  sharedPackageTypes_history
              where sharedPackageTypes_history.hostUID = old.uid;
  end if;
  
  if   (  new.sharedapps is not null  )  and
       ( (old.sharedapps is     null) or (old.sharedapps <> new.sharedapps) )
  then insert into  sharedPackageTypes_history ( hostUID, packageTypeId )
       select new.uid, packageTypes.packageTypeId
       from   packageTypes
       where  (new.sharedApps is not null) and (new.sharedApps <> '') and
              locate(packageTypes.packageTypeName, new.sharedApps);
  end if;
end;//


create trigger trig_hosts_history_delete_shared after delete on hosts_history
for each row
begin
  if   old.sharedapps is not null
  then delete from  sharedAppTypes_history
              where sharedAppTypes_history.hostUID = old.uid;
  end if;
  
  if   old.sharedapps is not null
  then delete from  sharedPackageTypes_history
              where sharedPackageTypes_history.hostUID = old.uid;
  end if;
end;//


-- ---------------------------------------------------------------------------
select 'Table "datas_history"' as '';
-- Sync "statusId"   with "status"
--      "dataTypeId" with "type"
--      "osId"       with "os"
--      "cpuTypeId"  with "cpu"
-- ---------------------------------------------------------------------------
create trigger trig_datas_history_insert_status_type_os_cpu before insert on datas_history
for each row
begin
  if   new.status is not null
  then
    set @statusId =
           ( select statuses.statusId
             from   statuses
             where  statuses.statusName = new.status );
    if (select @statusId) is not null
    then set new.statusId = @statusId;
    end if;
  end if;
  
  if   new.type is not null
  then
    set @dataTypeId =
           ( select dataTypes.dataTypeId
             from   dataTypes
             where  dataTypes.dataTypeName = new.type );
    if (select @dataTypeId) is not null
    then set new.dataTypeId = @dataTypeId;
    end if;
  end if;
  
  if   new.os is not null
  then
    set @osId =
           ( select oses.osId
             from   oses
             where  oses.osName = new.os );
    if (select @osId) is not null
    then set new.osId = @osId;
    end if;
  end if;
  
  if   new.cpu is not null
  then
    set @cpuTypeId =
           ( select cpuTypes.cpuTypeId
             from   cpuTypes
             where  cpuTypes.cpuTypeName = new.cpu );
    if (select @cpuTypeId) is not null
    then set new.cpuTypeId = @cpuTypeId;
    end if;
  end if;
end;//


create trigger trig_datas_history_update_status_type_os_cpu before update on datas_history
for each row
begin
  if   (new.status is null) and (old.status is not null)
  then set new.statusId = null;
  end if;
  
  if   (  new.status is not null  )  and
       ( (old.status is     null) or (old.status <> new.status) )
  then
    set @statusId =
           ( select statuses.statusId
             from   statuses
             where  statuses.statusName = new.status );
    if (select @statusId) is not null
    then set new.statusId = @statusId;
    end if;
  end if;
  
  if   (new.type is null) and (old.type is not null)
  then set new.dataTypeId = null;
  end if;
  
  if   (  new.type is not null  )  and
       ( (old.type is     null) or (old.type <> new.type) )
  then
    set @dataTypeId =
           ( select dataTypes.dataTypeId
             from   dataTypes
             where  dataTypes.dataTypeName = new.type );
    if (select @dataTypeId) is not null
    then set new.dataTypeId = @dataTypeId;
    end if;
  end if;
  
  if   (new.os is null) and (old.os is not null)
  then set new.osId = null;
  end if;
  
  if   (  new.os is not null  )  and
       ( (old.os is     null) or (old.os <> new.os) )
  then
    set @osId =
           ( select oses.osId
             from   oses
             where  oses.osName = new.os );
    if (select @osId) is not null
    then set new.osId = @osId;
    end if;
  end if;
  
  if   (  new.cpu is not null  )  and
       ( (old.cpu is     null) or (old.cpu <> new.cpu) )
  then
    set @cpuTypeId =
           ( select cpuTypes.cpuTypeId
             from   cpuTypes
             where  cpuTypes.cpuTypeName = new.cpu );
    if (select @cpuTypeId) is not null
    then set new.cpuTypeId = @cpuTypeId;
    end if;
  end if;
end;//


-- ---------------------------------------------------------------------------
select 'Table "works_history"' as '';
-- Sync "statusId" with "status"
-- ---------------------------------------------------------------------------
create trigger trig_works_history_insert_status before insert on works_history
for each row
begin
  if   new.status is not null
  then
    set @statusId =
           ( select statuses.statusId
             from   statuses
             where  statuses.statusName = new.status );
    if (select @statusId) is not null
    then set new.statusId = @statusId;
    end if;
  end if;
end;//

create trigger trig_works_history_update_status before update on works_history
for each row
begin
  if   (new.status is null) and (old.status is not null)
  then set new.statusId = null;
  end if;
  
  if   (  new.status is not null  )  and
       ( (old.status is     null) or (old.status <> new.status) )
  then
    set @statusId =
           ( select statuses.statusId
             from   statuses
             where  statuses.statusName = new.status );
    if (select @statusId) is not null
    then set new.statusId = @statusId;
    end if;
  end if;
end;//


-- ---------------------------------------------------------------------------
select 'Table "tasks_history"' as '';
-- Sync "statusId" with "status"
-- ---------------------------------------------------------------------------
create trigger trig_tasks_history_insert_status before insert on tasks_history
for each row
begin
  if   new.status is not null
  then
    set @statusId =
           ( select statuses.statusId
             from   statuses
             where  statuses.statusName = new.status );
    if (select @statusId) is not null
    then set new.statusId = @statusId;
    end if;
  end if;
end;//

create trigger trig_tasks_history_update_status before update on tasks_history
for each row
begin
  if   (new.status is null) and (old.status is not null)
  then set new.statusId = null;
  end if;
  
  if   (  new.status is not null  )  and
       ( (old.status is     null) or (old.status <> new.status) )
  then
    set @statusId =
           ( select statuses.statusId
             from   statuses
             where  statuses.statusName = new.status );
    if (select @statusId) is not null
    then set new.statusId = @statusId;
    end if;
  end if;
end;//


-- ===========================================================================
select 'Table "apps_history"' as '';
-- Sync Column "appTypeId"           with "type"
--             "packageTypeId"       with "neededpackages"
--      Table  "executables_history" with "os_cpuURI"
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- Procedure to insert "apps_history.os_cpuURI" in table "executables_history"
-- ---------------------------------------------------------------------------
drop   procedure if exists proc_insert_app_hist_os_cpu_uri_in_executables_hist //

create procedure           proc_insert_app_hist_os_cpu_uri_in_executables_hist
( in APP_UID   char(36),
  in DATA_TYPE varchar(254),
  in DATA_OS   char(7),
  in DATA_CPU  char(7),
  in DATA_URI  varchar(254) )
begin
  if  DATA_URI is not null
  then
    set  @DATA_UID = right(DATA_URI, 36);
    
    if   @DATA_UID not in (select datas.uid from datas)
    then set @DATA_UID = null;
    end if;
    
    insert into executables_history ( appUID, dataTypeId, osId, cpuTypeId, dataUID, dataURI )
    select APP_UID,
           dataTypes.dataTypeId,
           oses.osId,
           cpuTypes.cpuTypeId,
           @DATA_UID,
           DATA_URI
    from   dataTypes,
           oses,
           cpuTypes
    where  (dataTypes.dataTypeName = DATA_TYPE) and
           (oses.osName            = DATA_OS)   and
           (cpuTypes.cpuTypeName   = DATA_CPU);
  end if;
end;//

-- ---------------------------------------------------------------------------
-- Procedure to delete "apps_history.os_cpuURI" in table "executables_history"
-- ---------------------------------------------------------------------------
drop   procedure if exists proc_delete_app_hist_os_cpu_uri_in_executables_hist //

create procedure           proc_delete_app_hist_os_cpu_uri_in_executables_hist
( in APP_UID       char(36),
  in DATA_TYPE     varchar(254),
  in DATA_OS       char(7),
  in DATA_CPU      char(7),
  in DATA_URI_OLD  varchar(254),
  in DATA_URI_NEW  varchar(254) )
begin
  if   (  DATA_URI_OLD is not null  ) and
       ( (DATA_URI_NEW is     null) or
         (DATA_URI_NEW <> DATA_URI_OLD) )
  then
    delete     executables_history
    from       executables_history
    inner join dataTypes on dataTypes.dataTypeId = executables_history.dataTypeId
    inner join oses      on oses.osId            = executables_history.osId
    inner join cpuTypes  on cpuTypes.cpuTypeId   = executables_history.cpuTypeId
    where      (executables_history.appUID       = APP_UID)   and
               (dataTypes.dataTypeName           = DATA_TYPE) and
               (oses.osName                      = DATA_OS)   and
               (cpuTypes.cpuTypeName             = DATA_CPU)  and
               (executables_history.dataURI      = DATA_URI_OLD);
  end if;
end;//

-- ---------------------------------------------------------------------------
-- Procedure to update "apps_history.os_cpuURI" in table "executables_history"
-- ---------------------------------------------------------------------------
drop   procedure if exists proc_update_app_hist_os_cpu_uri_in_executables_hist //

create procedure           proc_update_app_hist_os_cpu_uri_in_executables_hist
( in APP_UID       char(36),
  in DATA_TYPE     varchar(254),
  in DATA_OS       char(7),
  in DATA_CPU      char(7),
  in DATA_URI_OLD  varchar(254),
  in DATA_URI_NEW  varchar(254) )
begin
  if   (  DATA_URI_NEW is not null  ) and
       ( (DATA_URI_OLD is     null) or
         (DATA_URI_OLD <> DATA_URI_NEW) )
  then
    set  @DATA_UID = right(DATA_URI_NEW, 36);
    
    if   @DATA_UID not in (select datas.uid from datas)
    then set @DATA_UID = null;
    end if;
    
    insert into executables_history ( appUID, dataTypeId, osId, cpuTypeId, dataUID, dataURI )
    select APP_UID,
           dataTypes.dataTypeId,
           oses.osId,
           cpuTypes.cpuTypeId,
           @DATA_UID,
           DATA_URI_NEW
    from   dataTypes,
           oses,
           cpuTypes
    where  (dataTypes.dataTypeName = DATA_TYPE) and
           (oses.osName            = DATA_OS)   and
           (cpuTypes.cpuTypeName   = DATA_CPU);
  end if;
end;//

-- ---------------------------------------------------------------------------
-- Table "apps_history" :  BEFORE INSERT :  Sync column "appTypeId"     with "type"
--                                                      "packageTypeId" with "neededpackages"
-- ---------------------------------------------------------------------------
create trigger trig_apps_history_before_insert before insert on apps_history
for each row
begin
  if   new.type is not null
  then set new.appTypeId =
           ( select appTypes.appTypeId
             from   appTypes
             where  appTypes.appTypeName = new.type );
  end if;
  
  if   new.neededpackages is not null
  then set new.packageTypeId =
           ( select packageTypes.packageTypeId
             from   packageTypes
             where  packageTypes.packageTypeName = new.neededpackages );
  end if;
end;//


-- ---------------------------------------------------------------------------
-- Table "apps_history" :  AFTER INSERT :  Sync table "executables_history" with "os_cpuURI"
-- ---------------------------------------------------------------------------
create trigger trig_apps_history_after_insert after insert on apps_history
for each row
begin
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'LINUX',  'IX86',   new.linux_ix86URI   );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'LINUX',  'AMD64',  new.linux_amd64URI  );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'LINUX',  'X86_64', new.linux_x86_64URI );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'LINUX',  'IA64',   new.linux_ia64URI   );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'LINUX',  'PPC',    new.linux_ppcURI    );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'MACOSX', 'IX86',   new.macos_ix86URI   );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'MACOSX', 'X86_64', new.macos_x86_64URI );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'MACOSX', 'PPC',    new.macos_ppcURI    );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'WIN32',  'IX86',   new.win32_ix86URI   );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'WIN32',  'AMD64',  new.win32_amd64URI  );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'WIN32',  'X86_64', new.win32_x86_64URI );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'JAVA',   'ALL',    new.javaURI         );
  
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'LINUX',  'IX86',   new.ldlinux_ix86URI   );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'LINUX',  'AMD64',  new.ldlinux_amd64URI  );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'LINUX',  'X86_64', new.ldlinux_x86_64URI );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'LINUX',  'IA64',   new.ldlinux_ia64URI   );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'LINUX',  'PPC',    new.ldlinux_ppcURI    );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'MACOSX', 'IX86',   new.ldmacos_ix86URI   );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'MACOSX', 'X86_64', new.ldmacos_x86_64URI );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'MACOSX', 'PPC',    new.ldmacos_ppcURI    );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'WIN32',  'IX86',   new.ldwin32_ix86URI   );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'WIN32',  'AMD64',  new.ldwin32_amd64URI  );
  call  proc_insert_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'WIN32',  'X86_64', new.ldwin32_x86_64URI );
end;//


-- ---------------------------------------------------------------------------
-- Table "apps_history" :  BEFORE UPDATE :  - Sync column "appTypeId"     with "type"
--                                                        "packageTypeId" with "neededpackages"
--                                          - Sync table  "executables_history" with "os_cpuURI"
-- ---------------------------------------------------------------------------
create trigger trig_apps_history_before_update before update on apps_history
for each row
begin
  
  if   (new.type is null) and (old.type is not null)
  then set new.appTypeId = null;
  end if;
  
  if   (  new.type is not null  )  and
       ( (old.type is     null) or (old.type <> new.type) )
  then set new.appTypeId =
           ( select appTypes.appTypeId
             from   appTypes
             where  appTypes.appTypeName = new.type );
  end if;
  
  if   (new.neededpackages is null) and (old.neededpackages is not null)
  then set new.packageTypeId = null;
  end if;
  
  if   (  new.neededpackages is not null  )  and
       ( (old.neededpackages is     null) or (old.neededpackages <> new.neededpackages) )
  then set new.packageTypeId =
           ( select packageTypes.packageTypeId
             from   packageTypes
             where  packageTypes.packageTypeName = new.neededpackages );
  end if;
  
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'LINUX',  'IX86',   old.linux_ix86URI,     new.linux_ix86URI   );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'LINUX',  'AMD64',  old.linux_amd64URI,    new.linux_amd64URI  );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'LINUX',  'X86_64', old.linux_x86_64URI,   new.linux_x86_64URI );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'LINUX',  'IA64',   old.linux_ia64URI,     new.linux_ia64URI   );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'LINUX',  'PPC',    old.linux_ppcURI,      new.linux_ppcURI    );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'MACOSX', 'IX86',   old.macos_ix86URI,     new.macos_ix86URI   );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'MACOSX', 'X86_64', old.macos_x86_64URI,   new.macos_x86_64URI );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'MACOSX', 'PPC',    old.macos_ppcURI,      new.macos_ppcURI    );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'WIN32',  'IX86',   old.win32_ix86URI,     new.win32_ix86URI   );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'WIN32',  'AMD64',  old.win32_amd64URI,    new.win32_amd64URI  );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'WIN32',  'X86_64', old.win32_x86_64URI,   new.win32_x86_64URI );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'JAVA',   'ALL',    old.javaURI,           new.javaURI         );
  
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'LINUX',  'IX86',   old.ldlinux_ix86URI,   new.ldlinux_ix86URI   );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'LINUX',  'AMD64',  old.ldlinux_amd64URI,  new.ldlinux_amd64URI  );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'LINUX',  'X86_64', old.ldlinux_x86_64URI, new.ldlinux_x86_64URI );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'LINUX',  'IA64',   old.ldlinux_ia64URI,   new.ldlinux_ia64URI   );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'LINUX',  'PPC',    old.ldlinux_ppcURI,    new.ldlinux_ppcURI    );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'MACOSX', 'IX86',   old.ldmacos_ix86URI,   new.ldmacos_ix86URI   );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'MACOSX', 'X86_64', old.ldmacos_x86_64URI, new.ldmacos_x86_64URI );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'MACOSX', 'PPC',    old.ldmacos_ppcURI,    new.ldmacos_ppcURI    );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'WIN32',  'IX86',   old.ldwin32_ix86URI,   new.ldwin32_ix86URI   );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'WIN32',  'AMD64',  old.ldwin32_amd64URI,  new.ldwin32_amd64URI  );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'WIN32',  'X86_64', old.ldwin32_x86_64URI, new.ldwin32_x86_64URI );
  
end;//


-- ---------------------------------------------------------------------------
-- Table "apps_history" :  AFTER UPDATE :  Sync table "executables_history" with "os_cpuURI"
-- ---------------------------------------------------------------------------
create trigger trig_apps_history_after_update after update on apps_history
for each row
begin
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'LINUX',  'IX86',   old.linux_ix86URI,     new.linux_ix86URI   );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'LINUX',  'AMD64',  old.linux_amd64URI,    new.linux_amd64URI  );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'LINUX',  'X86_64', old.linux_x86_64URI,   new.linux_x86_64URI );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'LINUX',  'IA64',   old.linux_ia64URI,     new.linux_ia64URI   );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'LINUX',  'PPC',    old.linux_ppcURI,      new.linux_ppcURI    );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'MACOSX', 'IX86',   old.macos_ix86URI,     new.macos_ix86URI   );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'MACOSX', 'X86_64', old.macos_x86_64URI,   new.macos_x86_64URI );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'MACOSX', 'PPC',    old.macos_ppcURI,      new.macos_ppcURI    );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'WIN32',  'IX86',   old.win32_ix86URI,     new.win32_ix86URI   );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'WIN32',  'AMD64',  old.win32_amd64URI,    new.win32_amd64URI  );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'WIN32',  'X86_64', old.win32_x86_64URI,   new.win32_x86_64URI );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'BINARY',  'JAVA',   'ALL',    old.javaURI,           new.javaURI         );
  
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'LINUX',  'IX86',   old.ldlinux_ix86URI,   new.ldlinux_ix86URI   );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'LINUX',  'AMD64',  old.ldlinux_amd64URI,  new.ldlinux_amd64URI  );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'LINUX',  'X86_64', old.ldlinux_x86_64URI, new.ldlinux_x86_64URI );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'LINUX',  'IA64',   old.ldlinux_ia64URI,   new.ldlinux_ia64URI   );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'LINUX',  'PPC',    old.ldlinux_ppcURI,    new.ldlinux_ppcURI    );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'MACOSX', 'IX86',   old.ldmacos_ix86URI,   new.ldmacos_ix86URI   );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'MACOSX', 'X86_64', old.ldmacos_x86_64URI, new.ldmacos_x86_64URI );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'MACOSX', 'PPC',    old.ldmacos_ppcURI,    new.ldmacos_ppcURI    );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'WIN32',  'IX86',   old.ldwin32_ix86URI,   new.ldwin32_ix86URI   );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'WIN32',  'AMD64',  old.ldwin32_amd64URI,  new.ldwin32_amd64URI  );
  call  proc_update_app_hist_os_cpu_uri_in_executables_hist ( new.uid, 'LIBRARY', 'WIN32',  'X86_64', old.ldwin32_x86_64URI, new.ldwin32_x86_64URI );
end;//


-- ---------------------------------------------------------------------------
-- Table "apps_history" :  BEFORE DELETE :  Sync table "executables_history" with "os_cpuURI"
-- ---------------------------------------------------------------------------
create trigger trig_apps_history_before_delete before delete on apps_history
for each row
begin
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'LINUX',  'IX86',   old.linux_ix86URI,     null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'LINUX',  'AMD64',  old.linux_amd64URI,    null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'LINUX',  'X86_64', old.linux_x86_64URI,   null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'LINUX',  'IA64',   old.linux_ia64URI,     null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'LINUX',  'PPC',    old.linux_ppcURI,      null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'MACOSX', 'IX86',   old.macos_ix86URI,     null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'MACOSX', 'X86_64', old.macos_x86_64URI,   null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'MACOSX', 'PPC',    old.macos_ppcURI,      null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'WIN32',  'IX86',   old.win32_ix86URI,     null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'WIN32',  'AMD64',  old.win32_amd64URI,    null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'WIN32',  'X86_64', old.win32_x86_64URI,   null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'BINARY',  'JAVA',   'ALL',    old.javaURI,           null );
  
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'LINUX',  'IX86',   old.ldlinux_ix86URI,   null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'LINUX',  'AMD64',  old.ldlinux_amd64URI,  null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'LINUX',  'X86_64', old.ldlinux_x86_64URI, null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'LINUX',  'IA64',   old.ldlinux_ia64URI,   null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'LINUX',  'PPC',    old.ldlinux_ppcURI,    null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'MACOSX', 'IX86',   old.ldmacos_ix86URI,   null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'MACOSX', 'X86_64', old.ldmacos_x86_64URI, null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'MACOSX', 'PPC',    old.ldmacos_ppcURI,    null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'WIN32',  'IX86',   old.ldwin32_ix86URI,   null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'WIN32',  'AMD64',  old.ldwin32_amd64URI,  null );
  call  proc_delete_app_hist_os_cpu_uri_in_executables_hist ( old.uid, 'LIBRARY', 'WIN32',  'X86_64', old.ldwin32_x86_64URI, null );
end;//


delimiter ;
