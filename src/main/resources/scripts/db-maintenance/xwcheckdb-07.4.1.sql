-- 
--  Copyrights     : CNRS
--  Author         : Oleg Lodygensky
--  Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
--  Web            : http://www.xtremweb-hep.org
--  
--       This file is part of XtremWeb-HEP.
-- 
--     XtremWeb-HEP is free software: you can redistribute it and/or modify
--     it under the terms of the GNU General Public License as published by
--     the Free Software Foundation, either version 3 of the License, or
--     (at your option) any later version.
-- 
--     XtremWeb-HEP is distributed in the hope that it will be useful,
--     but WITHOUT ANY WARRANTY; without even the implied warranty of
--     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
--     GNU General Public License for more details.
-- 
--     You should have received a copy of the GNU General Public License
--     along with XtremWeb-HEP.  If not, see <http://www.gnu.org/licenses/>.
-- 
-- 



--
-- Version : 7.4.1
--
-- File    : xwcheckdb-7.4.1.sql
-- Purpose : this file contains the needed SQL commands to 
--           test if DB is 7.4.1 compliant
--


-- 
-- Since XWHEP 7.4.1 :
--  * works.label is char(150)
--  * datas.name is char(200)

-- we always want to update
-- SELECT dummy FROM apps;
-- ALTER TABLE works MODIFY COLUMN label     char(150);

SELECT       column_type
       FROM  information_schema.columns
       WHERE column_name = "label"
        AND  table_name = "works" 
        AND  column_type="char(50)";
--
-- End Of File
--
