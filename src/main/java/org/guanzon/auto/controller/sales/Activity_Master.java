/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.auto.controller.sales;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.iface.GRecord;
import org.guanzon.appdriver.iface.GTransaction;
import org.guanzon.auto.general.CancelForm;
import org.guanzon.auto.general.SearchDialog;
import org.guanzon.auto.model.sales.Model_Activity_Master;
import org.guanzon.auto.validator.sales.ValidatorFactory;
import org.guanzon.auto.validator.sales.ValidatorInterface;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela
 */
public class Activity_Master implements GRecord {
    final String XML = "Model_Inquiry_Master.xml";
    GRider poGRider;
    String psBranchCd;
    boolean pbWtParent;
    int pnEditMode;
    String psTransStat;
    
    String psMessagex;
    public JSONObject poJSON;
    
    Model_Activity_Master poModel;
    ArrayList<Model_Activity_Master> paMaster;
    
    public Activity_Master(GRider foGRider, boolean fbWthParent, String fsBranchCd) {
        poGRider = foGRider;
        pbWtParent = fbWthParent;
        psBranchCd = fsBranchCd.isEmpty() ? foGRider.getBranchCode() : fsBranchCd;

        poModel = new Model_Activity_Master(foGRider);
        pnEditMode = EditMode.UNKNOWN;
    }

    @Override
    public int getEditMode() {
        return pnEditMode;
    }

    @Override
    public void setRecordStatus(String fsValue) {
        psTransStat = fsValue;
    }

    @Override
    public JSONObject setMaster(int fnCol, Object foData) {
        JSONObject obj = new JSONObject();
        obj.put("pnEditMode", pnEditMode);
        if (pnEditMode != EditMode.UNKNOWN){
            // Don't allow specific fields to assign values
            if(!(fnCol == poModel.getColumn("sTransNox") ||
                fnCol == poModel.getColumn("cRecdStat") ||
                fnCol == poModel.getColumn("sModified") ||
                fnCol == poModel.getColumn("dModified"))){
                poModel.setValue(fnCol, foData);
                obj.put(fnCol, pnEditMode);
            }
        }
        return obj;
    }

    @Override
    public JSONObject setMaster(String fsCol, Object foData) {
        return setMaster(poModel.getColumn(fsCol), foData);
    }
    
    public Object getMaster(int fnCol) {
        if(pnEditMode == EditMode.UNKNOWN)
            return null;
        else 
            return poModel.getValue(fnCol);
    }

    public Object getMaster(String fsCol) {
        return getMaster(poModel.getColumn(fsCol));
    }
    
    @Override
    public JSONObject newRecord() {
        poJSON = new JSONObject();
        try{
            pnEditMode = EditMode.ADDNEW;
            org.json.simple.JSONObject obj;

            poModel = new Model_Activity_Master(poGRider);
            Connection loConn = null;
            loConn = setConnection();
            poModel.setActvtyID(MiscUtil.getNextCode(poModel.getTable(), "sActvtyID", true, poGRider.getConnection(), poGRider.getBranchCode()+"AC"));
            poModel.setActNo(MiscUtil.getNextCode(poModel.getTable(), "sActNoxxx", true, poGRider.getConnection(), poGRider.getBranchCode()+"ACT"));
            poModel.newRecord();
            
            if (poModel == null){
                poJSON.put("result", "error");
                poJSON.put("message", "initialized new record failed.");
                return poJSON;
            }else{
                poJSON.put("result", "success");
                poJSON.put("message", "initialized new record.");
                pnEditMode = EditMode.ADDNEW;
            }
        }catch(NullPointerException e){
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        
        return poJSON;
    }
    
    private Connection setConnection(){
        Connection foConn;
        if (pbWtParent){
            foConn = (Connection) poGRider.getConnection();
            if (foConn == null) foConn = (Connection) poGRider.doConnect();
        }else foConn = (Connection) poGRider.doConnect();
        return foConn;
    }
    
    @Override
    public JSONObject openRecord(String fsValue) {
        pnEditMode = EditMode.READY;
        poJSON = new JSONObject();
        
        poModel = new Model_Activity_Master(poGRider);
        poJSON = poModel.openRecord(fsValue);
        
        return poJSON;
    }

    @Override
    public JSONObject updateRecord() {
        poJSON = new JSONObject();
        if (pnEditMode != EditMode.READY && pnEditMode != EditMode.UPDATE){
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid edit mode.");
            return poJSON;
        }
        pnEditMode = EditMode.UPDATE;
        poJSON.put("result", "success");
        poJSON.put("message", "Update mode success.");
        return poJSON;
    }

    @Override
    public JSONObject saveRecord() {
        poJSON = new JSONObject();  
        ValidatorInterface validator = ValidatorFactory.make( ValidatorFactory.TYPE.Activity_Master, poModel);
        validator.setGRider(poGRider);
        if (!validator.isEntryOkay()){
            poJSON.put("result", "error");
            poJSON.put("message", validator.getMessage());
            return poJSON;
        }
        
        poJSON =  poModel.saveRecord();
        if("error".equalsIgnoreCase((String)checkData(poJSON).get("result"))){
            if (!pbWtParent) poGRider.rollbackTrans();
            return checkData(poJSON);
        }
        
        return poJSON;
    }

    @Override
    public JSONObject deleteRecord(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JSONObject deactivateRecord(String fsValue) {
        poJSON = new JSONObject();

        if (poModel.getEditMode() == EditMode.UPDATE) {
            try {
                poJSON = poModel.setActive(false);
                
                if ("error".equals((String) poJSON.get("result"))) {
                    return poJSON;
                }
                
                ValidatorInterface validator = ValidatorFactory.make( ValidatorFactory.TYPE.Activity_Master, poModel);
                validator.setGRider(poGRider);
                if (!validator.isEntryOkay()){
                    poJSON.put("result", "error");
                    poJSON.put("message", validator.getMessage());
                    return poJSON;
                }

                CancelForm cancelform = new CancelForm();
                if (!cancelform.loadCancelWindow(poGRider, poModel.getActvtyID(), poModel.getActvtyID(), "ACTIVITY")) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Cancellation failed.");
                    return poJSON;
                }

                poJSON = poModel.saveRecord();
                if ("success".equals((String) poJSON.get("result"))) {
                    poJSON.put("result", "success");
                    poJSON.put("message", "Cancellation success.");
                } else {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Cancellation failed.");
                }
            } catch (SQLException ex) {
                Logger.getLogger(Activity_Master.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction loaded to update.");
        }
        return poJSON;
    }

    @Override
    public JSONObject activateRecord(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public JSONObject searchRecord(String fsValue, boolean fbByCode) {
        String lsHeader = "Start Date»End Date»Activity No»Activity Title";
        String lsColName = "dDateFrom»dDateThru»sActNoxxx»sActTitle";
        String lsSQL =  poModel.getSQL(); ;  
        
        if (fbByCode){
            lsSQL = MiscUtil.addCondition(lsSQL, " a.sActNoxxx = " + SQLUtil.toSQL(fsValue));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, " a.sActTitle LIKE " + SQLUtil.toSQL(fsValue + "%"));
        }
        
        System.out.println(lsSQL);
        JSONObject loJSON = SearchDialog.jsonSearch(
                    poGRider,
                    lsSQL,
                    fsValue,
                    lsHeader,
                    lsColName,
                "0.1D»0.1D»0.3D»0.5D", 
                    "ACTIVITY",
                    0);
            
        if (loJSON != null && !"error".equals((String) loJSON.get("result"))) {
        }else {
            loJSON = new JSONObject();
            loJSON.put("result", "error");
            loJSON.put("message", "No Transaction loaded.");
            return loJSON;
        }
        return loJSON;
    }
    
    @Override
    public Model_Activity_Master getModel() {
        return poModel;
    }
    
    private JSONObject checkData(JSONObject joValue){
        if(pnEditMode == EditMode.READY || pnEditMode == EditMode.UPDATE){
            if(joValue.containsKey("continue")){
                if(true == (boolean)joValue.get("continue")){
                    joValue.put("result", "success");
                    joValue.put("message", "Record saved successfully.");
                }
            }
        }
        return joValue;
    }
    
    public JSONObject searchDepartment(String fsValue) {
        poJSON = new JSONObject();
         
        String lsSQL =    " SELECT "
                        + " sDeptIDxx"
                        + " , sDeptName "
                        + " , cRecdStat "
                        + "FROM ggc_isysdbf.department ";
        
        lsSQL = MiscUtil.addCondition(lsSQL, " sDeptName LIKE " + SQLUtil.toSQL(fsValue + "%")
                                               + " AND cRecdStat = '1'");

        System.out.println("SEARCH DEPARTMENT: " + lsSQL);
        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                fsValue,
                "ID»Department",
                "sDeptIDxx»sDeptName",
                "sDeptIDxx»sDeptName",
                1);

        if (poJSON != null) {
            if(!"error".equals((String) poJSON.get("result"))){
                poModel.setDeptID((String) poJSON.get("sDeptIDxx"));
                poModel.setDeptName((String) poJSON.get("sDeptName"));
            }
        } else {
            poModel.setDeptID("");
            poModel.setDeptName("");
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
        
        return poJSON;
    }
    
    private String getSQ_Employee() {
        return " SELECT "
                + " c.sCompnyNm  "
                + " ,a.sEmployID "
                + " ,b.sDeptName "
                + " ,a.sDeptIDxx "
                + " ,e.sBranchNm "
                + " FROM GGC_ISysDBF.Employee_Master001 a	"
                + " LEFT JOIN GGC_ISysDBF.Department b ON  b.sDeptIDxx = a.sDeptIDxx "
                + " LEFT JOIN GGC_ISysDBF.Client_Master c on c.sClientID = a.sEmployID "
                + " LEFT JOIN GGC_ISysDBF.Branch_Others d ON d.sBranchCD = a.sBranchCd "
                + " LEFT JOIN GGC_ISysDBF.Branch e ON e.sBranchCd = a.sBranchCd "
                + " WHERE a.cRecdStat = '1' AND ISNULL(a.dFiredxxx) " 
                + " AND d.cDivision = (SELECT cDivision FROM GGC_ISysDBF.Branch_Others WHERE sBranchCd = " +  SQLUtil.toSQL(psBranchCd) + ")";
    }
    
    public JSONObject searchEmployee(String fsValue) {
        poJSON = new JSONObject();
        
        String lsSQL = getSQ_Employee() + " AND c.sCompnyNm LIKE " + SQLUtil.toSQL(fsValue + "%") 
                                        + " AND a.sDeptIDxx = " + SQLUtil.toSQL(poModel.getDeptID());

        System.out.println("SEARCH EMPLOYEE: " + lsSQL);
        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                fsValue,
                    "Employee Name»Department Name»Branch",
                    "sCompnyNm»sDeptName»sBranchNm",
                    "c.sCompnyNm»b.sDeptName»e.sBranchNm",
                0);

        if (poJSON != null) {
            if(!"error".equals((String) poJSON.get("result"))){
                poModel.setEmployID((String) poJSON.get("sEmployID"));
                poModel.setEmpInCharge((String) poJSON.get("sCompnyNm"));
            }
        } else {
            poModel.setEmployID("");
            poModel.setEmpInCharge("");
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
        
        return poJSON;
    }
    
    public JSONObject searchBranch(String fsValue) {
        poJSON = new JSONObject();
        
        String lsSQL =   " SELECT "
                + " IFNULL(a.sBranchCd, '') sBranchCd "
                + " , IFNULL(a.sBranchNm, '') sBranchNm "
                + " , IFNULL(b.cDivision, '') cDivision "
                + " FROM branch a "
                + " LEFT JOIN branch_others b ON a.sBranchCd = b.sBranchCd  "
                + " WHERE a.cRecdStat = '1'  "
                + " AND b.cDivision = (SELECT cDivision FROM branch_others WHERE sBranchCd = " + SQLUtil.toSQL(psBranchCd) + ")"
                + " AND sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%");

        System.out.println("SEARCH BRANCH: " + lsSQL);
        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                fsValue,
                    "Branch Code»Branch Name",
                    "sBranchCd»sBranchNm",
                    "sBranchCd»sBranchNm",
                1);

        if (poJSON != null) {
            if(!"error".equals((String) poJSON.get("result"))){
                poModel.setLocation((String) poJSON.get("sBranchCd"));
                poModel.setBranchNm((String) poJSON.get("sBranchNm"));
            }
        } else {
            poModel.setLocation("");
            poModel.setBranchNm("");
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
        
        return poJSON;
    }
    
    public JSONObject searchEventType(String fsValue) {
        poJSON = new JSONObject();
        
        String lsSQL =    " SELECT"
                        + " sActTypID "
                        + " ,sActTypDs "
                        + " ,sEventTyp "
                        + " ,cRecdStat "
                        + " FROM event_type "
                        + " WHERE sActTypDs LIKE " + SQLUtil.toSQL(fsValue + "%")
                        + " AND sEventTyp = " + SQLUtil.toSQL(poModel.getEventTyp());

        System.out.println("SEARCH EVENT TYPE: " + lsSQL);
        poJSON = ShowDialogFX.Search(poGRider,
                lsSQL,
                fsValue,
                    "Source",
                    "sActTypDs",
                    "sActTypDs",
                0);

        if (poJSON != null) {
            if(!"error".equals((String) poJSON.get("result"))){
                poModel.setActTypID((String) poJSON.get("sActTypID"));
                poModel.setEventTyp((String) poJSON.get("sEventTyp"));
                poModel.setActTypDs((String) poJSON.get("sActTypDs"));
                poModel.setActSrce((String) poJSON.get("sActTypDs"));
            } else {
                poModel.setActTypID("");
                poModel.setEventTyp("");
                poModel.setActTypDs("");
                poModel.setActSrce("");
            }
        } else {
            poModel.setActTypID("");
            poModel.setEventTyp("");
            poModel.setActTypDs("");
            poModel.setActSrce("");
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
        
        return poJSON;
    }
    
    public JSONObject ApproveRecord(String fsValue) {
        poJSON = new JSONObject();

        if (poModel.getEditMode() == EditMode.UPDATE) {
            poJSON = poModel.setActive(true);

            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }
            
//            poJSON = validateEntry();
//            if ("error".equals((String) poJSON.get("result"))) {
//                return poJSON;
//            }

            poJSON = poModel.saveRecord();
            if ("success".equals((String) poJSON.get("result"))) {
                poJSON.put("result", "success");
                poJSON.put("message", "Cancellation success.");
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Cancellation failed.");
            }
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction loaded to update.");
        }
        return poJSON;
    }
    
    public JSONObject loadTransactionForApproval() {
        poJSON = new JSONObject();
        if (poGRider == null) {
            poJSON.put("result", "error");
            poJSON.put("message", "Application driver is not set.");
            return poJSON;
        }

        String lsSQL = MiscUtil.addCondition(poModel.getSQL(), " a.cTranStat <> '1' AND (a.sApproved IS NULL OR a.sApproved = '') ORDER BY dDateFrom DESC ");
        System.out.println(lsSQL);
//        
//        loRS = poGRider.executeQuery(lsSQL);
//        poModel = factory.createCachedRowSet();
//        poModel.populate(loRS);
//        MiscUtil.close(loRS);

        return poJSON;
    
    }
    
    public JSONObject validateExistingRecord(){
        JSONObject loJSON = new JSONObject();
        String lsID = "";
        String lsDesc  = "";
        try {
            String lsSQL =    "   SELECT "                                                  
                            + "   a.sActvtyID "                                             
                            + " , a.sActNoxxx "                                             
                            + " , a.sActTitle "                                             
                            + " , a.sActTypID "                                              
                            + " , a.dDateFrom "                                             
                            + " , a.dDateThru "                                             
                            + " , a.sLocation "                                             
                            + " , a.cTranStat "                                               
                            + "FROM activity_master a "   ;                                  
            lsSQL = MiscUtil.addCondition(lsSQL, " REPLACE(a.sActTitle,' ','') = " + SQLUtil.toSQL(poModel.getActTitle().replace(" ",""))) +
                                                    " AND a.sActTypID = " + SQLUtil.toSQL(poModel.getActTypID()) +
                                                    " AND a.dDateFrom = " + SQLUtil.toSQL(xsDateShort((Date) poModel.getValue("dDateFrom")))+
                                                    " AND a.dDateThru = " + SQLUtil.toSQL(xsDateShort((Date) poModel.getValue("dDateThru")))+
                                                    " AND a.sLocation = " + SQLUtil.toSQL(poModel.getLocation())+
                                                    " AND a.cTranStat = '1' " +
                                                    " AND a.sActvtyID <> " + SQLUtil.toSQL(poModel.getActvtyID()) ;
            System.out.println("EXISTING ACTIVITY CHECK: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);

            if (MiscUtil.RecordCount(loRS) > 0){
                    while(loRS.next()){
                        lsID = loRS.getString("sActvtyID");
                        lsDesc = loRS.getString("sActTitle");
                    }
                    
                    MiscUtil.close(loRS);
                    
                    loJSON.put("result", "error") ;
                    loJSON.put("message","Found an existing acivity record for\n" + lsDesc.toUpperCase() + " <Activity ID:" + lsID + ">\n\n Do you want to view the record?");
                    loJSON.put("sActvtyID", lsID) ;
                    return loJSON;
            }
        
        } catch (SQLException ex) {
            Logger.getLogger(Activity_Master.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException e) {
            // Handle the NullPointerException
            loJSON.put("result", "") ;
            System.out.println("Caught a NullPointerException: " + e.getMessage());
        }
    
        return loJSON;
    }
    
    public static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }
}
