package com.example.workshop1.SQLite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.workshop1.Utils.PasswordEncryption;

public class Mysqliteopenhelper extends SQLiteOpenHelper {

    private static final String DBNAME="Mydb";

//    public Mysqliteopenhelper(@Nullable Context context) {
//        super(context, DBNAME, null, 1);
//    }
    public Mysqliteopenhelper(@Nullable Context context) {
        super(context, DBNAME, null, 12);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE Users (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username varchar(255), password varchar(255)," +
                "name varchar(255), type varchar(16), balance INTEGER)";
        db.execSQL(createUsers);

        String createTransactions = "CREATE TABLE Transactions (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "datetime TEXT, source INTEGER, destination INTEGER, amount INTEGER, " +
                "erid INTEGER, ttype varchar(2))";
        db.execSQL(createTransactions);

        String createRewards = "CREATE TABLE Rewards (_id INTEGER PRIMARY KEY, " +
                "name varchar(255), description varchar(1000), value INTEGER," +
                "uid INTEGER REFERENCES Users(_id))";
        db.execSQL(createRewards);

        String createEvents = "CREATE TABLE Events (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "description varchar(2000), reward INTEGER)";
        db.execSQL(createEvents);

        String createStudentRewards = "CREATE TABLE StudentRewards (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "uid INTEGER REFERENCES Users(_id)," +
                "rid INTEGER REFERENCES Rewards(_id))";
        db.execSQL(createStudentRewards);

        // Archive tables for events and rewards
        String createRewardsA = "CREATE TABLE RewardsA (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name varchar(255), description varchar(1000), value INTEGER," +
                "uid INTEGER REFERENCES Users(_id))";
        db.execSQL(createRewardsA);

        String createEventsA = "CREATE TABLE EventsA (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "description varchar(2000), reward INTEGER)";
        db.execSQL(createEventsA);


        // Approval list
        String createVendorApproval = "CREATE TABLE IF NOT EXISTS VendorApproval (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username varchar(255), password varchar(255)," +
                "name varchar(255), approved INTEGER DEFAULT 0)";
        db.execSQL(createVendorApproval);

        // Create admin account with encrypted password
        String adminPassword = PasswordEncryption.encrypt("admin123");
        if (adminPassword == null) {
            Log.e("SQL", "Failed to encrypt admin password");
            adminPassword = ""; // 如果加密失败，使用空密码，但这种情况不应该发生
        }
        String addAdmin = "INSERT INTO Users VALUES(1, 'admin', '" + adminPassword + "','HKU TokenFlow Admin', 'admin', 0)";
        db.execSQL(addAdmin);

        // accounts for testing
        /*String addVendor1 = "INSERT INTO Users VALUES(2, 'v1', 'password','vender1 name', 'vendor', 0)";
        db.execSQL(addVendor1);
        String addVendor2 = "INSERT INTO Users VALUES(3, 'v2', 'password','vender2 name', 'vendor', 0)";
        db.execSQL(addVendor2);
        String addStudent = "INSERT INTO Users VALUES(4, 'student', 'password','student', 'student', 0)";
        db.execSQL(addStudent);*/

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 9) {
            // 创建 VendorApproval 表（如果不存在）
            String createVendorApproval = "CREATE TABLE IF NOT EXISTS VendorApproval (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username varchar(255), password varchar(255)," +
                    "name varchar(255), approved INTEGER DEFAULT 0)";
            db.execSQL(createVendorApproval);
        }
        // Drop the existing tables
        db.execSQL("DROP TABLE IF EXISTS Users;");
        db.execSQL("DROP TABLE IF EXISTS Transactions;");
        db.execSQL("DROP TABLE IF EXISTS Rewards;");
        db.execSQL("DROP TABLE IF EXISTS Events;");
        db.execSQL("DROP TABLE IF EXISTS StudentRewards;");
        db.execSQL("DROP TABLE IF EXISTS RewardsA;");
        db.execSQL("DROP TABLE IF EXISTS EventsA;");
        db.execSQL("DROP TABLE IF EXISTS VendorApproval;");

        // Call onCreate to recreate the tables
        onCreate(db);
    }

    // Add user (student from register/vendor from admin)
    public long addUser(User user){
        SQLiteDatabase db=getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("username", user.getUsername());
        contentValues.put("password", user.getPassword());
        contentValues.put("name", user.getName());
        contentValues.put("type", user.getType());
        contentValues.put("balance", user.getBalance());

        return db.insert("Users",null, contentValues);
    }

    // Add VendorApproval
    public long addVendorApproval(VendorApproval vendorApproval){
        SQLiteDatabase db=getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("username", vendorApproval.getUsername());
        contentValues.put("password", vendorApproval.getPassword());
        contentValues.put("name", vendorApproval.getName());
        contentValues.put("approved", vendorApproval.getApproved());

        return db.insert("VendorApproval",null, contentValues);
    }

    // login
    public User login(String name, String password){
        SQLiteDatabase db1 = getWritableDatabase();
        Cursor users = db1.query("Users", null, "username like?", new String[]{name}, null, null, null);
        if (users != null && users.moveToNext()) {
            String dbpwd = users.getString(2);  // 获取加密后的密码
            String username = users.getString(1);
            String uname = users.getString(3);
            String type = users.getString(4);
            int balance = users.getInt(5);

            // 返回用户对象，密码验证将在 LoginActivity 中进行
            return new User(username, dbpwd, uname, type, balance);
        }
        return null;  // unsuccessful login
    }

    // ------------------ USER ------------------
    // get user id from username and password
    public int getUserId(String username, String pwd) {
        SQLiteDatabase db1 = getWritableDatabase();
        Cursor id =  db1.query("Users", new String[]{"_id"}, "username = ? AND password = ?", new String[] {username, pwd}, null, null, null);
        if (id != null && id.moveToFirst()) {
            return id.getInt(0);
        } else {
            return -999;
        }
    }

    // get all transactions of a user
    public Cursor getUserTrans(int uid) {
        SQLiteDatabase db1 = getWritableDatabase();
        return db1.query("Transactions", null, "source = ? OR destination = ?", new String[] {String.valueOf(uid), String.valueOf(uid)}, null, null, "datetime DESC");
    }

    // get type of user given id
    public String getUserType(int uid) {
        SQLiteDatabase db1 = getWritableDatabase();
        Cursor cur = db1.query("Users", new String[]{"type"}, "_id = ?", new String[] {String.valueOf(uid)}, null, null, null);
        if (cur != null && cur.moveToNext()) {
            String type =  cur.getString(0);
            if (type.equals("admin")) {
                return "(a)";
            } else if (type.equals("student")) {
                return "(s)";
            } else {
                return "(v)";
            }
        }
        return "(?)";
    }


    // ------------------ VENDOR ------------------
    // get vendor list
    public Cursor getVendors() {
        SQLiteDatabase db1 = getWritableDatabase();
        return db1.query("Users", null, "type like?", new String[]{"vendor"}, null, null, "name");
    }

    // edit vendor name
    public void editVendorName(String name, String username) {
        SQLiteDatabase db1 = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        db1.update("Users", cv, "username = ?", new String[] {username});
    }

    // edit vendor password
    public void editVendorPwd(String name, String pwd) {
        SQLiteDatabase db1 = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("password", pwd);
        db1.update("Users", cv, "username = ?", new String[] {name});
    }

    // delete vendor
    public void deleteVendor(String username) {
        SQLiteDatabase db1 = getWritableDatabase();
        db1.delete("Users", "username = ? AND type LIKE 'vendor'", new String[] {username});
    }

    // ------------------ EVENTS ------------------
    // Add event
    public long addEvent(Event event){
        SQLiteDatabase db=getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("description", event.getDescription());
        contentValues.put("reward", event.getReward());
        long resA = db.insert("EventsA",null, contentValues);
        if (resA == -1) {
            Log.e("SQL addEvent", "Failed to insert into RewardsA");
        } else {
            Log.d("SQL addEvent", "Insert into EventsA sucessful");
        }

        ContentValues contentValues2 = new ContentValues();
        contentValues2.put("description", event.getDescription());
        contentValues2.put("reward", event.getReward());
        long res = db.insert("Events",null, contentValues2);
        if (res == -1) {
            Log.e("SQL addEvent", "Failed to insert into Rewards");
        } else {
            Log.d("SQL addEvent", "Insert into Events sucessful");
        }
        return res;
    }

    // get current events list
    public Cursor getEvents() {
        SQLiteDatabase db1 = getWritableDatabase();
        return db1.query("Events", null, null, null, null, null, "description");
    }

    // edit event name
    public void editEvent(String newName, int newToken, String orgName, int orgToken) {
        SQLiteDatabase db1 = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("description", newName);
        cv.put("reward", newToken);
        db1.update("Events", cv, "description = ? AND reward = ?", new String[] {orgName, String.valueOf(orgToken)});

        ContentValues cvA = new ContentValues();
        cvA.put("description", newName);
        cvA.put("reward", newToken);
        db1.update("EventsA", cvA, "description = ? AND reward = ?", new String[] {orgName, String.valueOf(orgToken)});
    }

    // get event id from description and reward
    public int getEventId(String orgName, int orgToken) {
        SQLiteDatabase db1 = getWritableDatabase();
        Cursor id =  db1.query("EventsA", new String[]{"_id"}, "description = ? AND reward = ?", new String[] {orgName, String.valueOf(orgToken)}, null, null, null);
        if (id != null && id.moveToFirst()) {
            return id.getInt(0);
        } else {
            return -999;
        }
    }

    // get event reward from eid
    public int getEventReward(int eid) {
        SQLiteDatabase db1 = getWritableDatabase();
        Cursor reward =  db1.query("EventsA", new String[]{"reward"}, "_id = ?", new String[] {String.valueOf(eid)}, null, null, null);
        if (reward != null && reward.moveToFirst()) {
            return reward.getInt(0);
        } else {
            return -999;
        }
    }

    // get event name from eid
    public String getEventName(int eid) {
        SQLiteDatabase db1 = getWritableDatabase();
        Cursor desc =  db1.query("EventsA", new String[]{"description"}, "_id = ?", new String[] {String.valueOf(eid)}, null, null, null);
        if (desc != null && desc.moveToFirst()) {
            return desc.getString(0);
        } else {
            return "";
        }
    }

    // delete event
    public void deleteEvent(String orgName, int orgToken) {
        SQLiteDatabase db1 = getWritableDatabase();
        db1.delete("Events", "description = ? AND reward = ?", new String[] {orgName, String.valueOf(orgToken)});
    }


    // ------------------ REWARDS ------------------
    // get full current rewards list
    public Cursor getRewardsAll() {
        SQLiteDatabase db1 = getWritableDatabase();
        return db1.query("Rewards", null, null, null, null, null, null);
    }

    // get current rewards list by vendor
    public Cursor getRewardsVendor(int uid) {
        SQLiteDatabase db1 = getWritableDatabase();
        return db1.query("Rewards", null, "uid = ?", new String[]{String.valueOf(uid)}, null, null, null);
    }

    // Add reward
    public long addReward(Reward reward){
        SQLiteDatabase db=getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", reward.getName());
        contentValues.put("description", reward.getDescription());
        contentValues.put("value", reward.getValue());
        contentValues.put("uid", reward.getUid());
        long resA = db.insert("RewardsA",null, contentValues);
        if (resA == -1) {
            Log.e("SQL addEvent", "Failed to insert into RewardsA");
        } else {
            Log.d("SQL addEvent", "Insert into RewardsA sucessful");
        }

        ContentValues contentValues2 = new ContentValues();
        contentValues2.put("name", reward.getName());
        contentValues2.put("description", reward.getDescription());
        contentValues2.put("value", reward.getValue());
        contentValues2.put("uid", reward.getUid());
        long res = db.insert("Rewards",null, contentValues2);
        if (res == -1) {
            Log.e("SQL addEvent", "Failed to insert into Rewards");
        } else {
            Log.d("SQL addEvent", "Insert into Rewards sucessful");
        }
        return res;
    }

    // edit reward name
    public void editReward(String newName, String newDesc, int newToken, String orgName, String orgDesc) {
        SQLiteDatabase db1 = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", newName);
        cv.put("description", newDesc);
        cv.put("value", newToken);
        db1.update("Rewards", cv, "name = ? AND description = ?", new String[] {orgName, orgDesc});

        ContentValues cv2 = new ContentValues();
        cv2.put("name", newName);
        cv2.put("description", newDesc);
        cv2.put("value", newToken);
        db1.update("RewardsA", cv2, "name = ? AND description = ?", new String[] {orgName, orgDesc});
    }


    // delete reward voucher
    public void deleteReward(String orgName, String orgDesc) {
        SQLiteDatabase db1 = getWritableDatabase();
        db1.delete("Rewards", "name = ? AND description = ?", new String[] {orgName, orgDesc});
    }


    // get reward id from description and reward
    public int getRewardId(String name, String desc, int value, int uid) {
        SQLiteDatabase db1 = getWritableDatabase();
        Cursor id =  db1.query("RewardsA", new String[]{"_id"}, "name = ? AND description = ? AND value = ? AND uid = ?",
                new String[] {name, desc, String.valueOf(value), String.valueOf(uid)}, null, null, null);
        if (id != null && id.moveToFirst()) {
            return id.getInt(0);
        } else {
            return -999;
        }
    }

    // get reward name from rid
    public String getRewardName(int rid) {
        SQLiteDatabase db1 = getWritableDatabase();
        Cursor name =  db1.query("RewardsA", new String[]{"name"}, "_id = ?", new String[] {String.valueOf(rid)}, null, null, null);
        if (name != null && name.moveToFirst()) {
            return name.getString(0);
        } else {
            return "";
        }
    }

    // get reward obj from rid
    public Cursor getRewardFromId(int rid) {
        SQLiteDatabase db1 = getWritableDatabase();
        return db1.query("RewardsA", null, "_id = ?", new String[] {String.valueOf(rid)}, null, null, null);
    }


    // ------------------ TRANSACTIONS ------------------
    // check if check-in id is valid (id exists in current Events table)
    public boolean checkValidEvent(int checkInId) {
        SQLiteDatabase db1 = getWritableDatabase();
        Cursor eids = db1.query("Events", new String[]{"_id"}, null, null, null, null, null);
        if (eids != null) {
            while (eids.moveToNext()) {
                int eid = eids.getInt(0);
                if (checkInId == eid) {
                    return true;
                }
            }
        }
        return false;
    }

    // check if check-in is repeated (if source,dest already exists in Transactions)
    public boolean checkRepeatedCheckIn(int checkInId, int srcId) {
        SQLiteDatabase db1 = getWritableDatabase();
        Cursor res = db1.query("Transactions", null, "erid = ? AND destination = ? AND ttype = ?", new String[] {String.valueOf(checkInId), String.valueOf(srcId), "e"}, null, null, null);
        Log.d("SQL",  "result length: " + res.getCount());
        return res.getCount() == 0;
    }

    // Add transaction
    public void addTransaction(Transaction trans){
        SQLiteDatabase db = this.getWritableDatabase();
        SQLiteDatabase db1 = this.getReadableDatabase();

        // update Users balances
        String sourceUid = String.valueOf(trans.getSource());
        Cursor cursor = db1.rawQuery("SELECT balance FROM Users WHERE _id = ?", new String[]{sourceUid});
        if (cursor != null && cursor.moveToFirst()) {
            int sourceBalance = cursor.getInt(0);
            int newSourceBalance = sourceBalance - trans.getAmount();
            ContentValues cvSrc = new ContentValues();
            cvSrc.put("balance", newSourceBalance);
            db.update("Users", cvSrc, "_id = ?", new String[] {sourceUid});
            Log.d("SQL", "source balance updated");
        } else {
            Log.d("SQL", "source user not found");
        }
        cursor.close();

        String destUid = String.valueOf(trans.getDestination());
        Cursor cursor1 = db1.rawQuery("SELECT balance FROM Users WHERE _id = ?", new String[]{destUid});
        if (cursor1 != null && cursor1.moveToFirst()) {
            int destBalance = cursor1.getInt(0);
            int newDestBalance = destBalance + trans.getAmount();
            ContentValues cvDest = new ContentValues();
            cvDest.put("balance", newDestBalance);
            db.update("Users", cvDest, "_id = ?", new String[] {destUid});
            Log.d("SQL", "dest balance updated");
        } else {
            Log.d("SQL", "dest user not found");
        }
        cursor1.close();

        // Add row to Transactions
        ContentValues contentValues = new ContentValues();
        contentValues.put("datetime", trans.getDatetime());
        contentValues.put("source", trans.getSource());
        contentValues.put("destination", trans.getDestination());
        contentValues.put("amount", trans.getAmount());
        contentValues.put("erid", trans.getErid());
        contentValues.put("ttype", trans.getTtype());
        db.insert("Transactions",null, contentValues);
    }

    // Get all transactions
    public Cursor getAllTrans() {
        SQLiteDatabase db1 = getWritableDatabase();
        return db1.query("Transactions", null, null, null, null, null, "datetime DESC");
    }


    // ------------------ STUDENTREWARDS ------------------
    // Add student-reward record (redeem reward)
    public void addStudentReward(StudentReward sr){
        SQLiteDatabase db=getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("uid", sr.getUid());
        contentValues.put("rid", sr.getRid());

        db.insert("StudentRewards",null, contentValues);
    }

    // Delete student-reward record (use reward voucher)
    public void deleteStudentReward(int uid, int rid) {
        SQLiteDatabase db1 = getWritableDatabase();
        db1.delete("StudentRewards", "uid = ? AND rid = ?", new String[] {String.valueOf(uid), String.valueOf(rid)});
    }

    // Get student reward records by student uid
    public Cursor getStudentRewards(int uid) {
        SQLiteDatabase db1 = getWritableDatabase();
        return db1.query("StudentRewards", null, "uid = ?", new String[]{String.valueOf(uid)}, null, null, null);
    }

    // ------------------ OTHERS ------------------
    // get balance of user at time of call
    public int getUserBalance(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("Users", new String[]{"balance"}, "_id = ?", new String[]{String.valueOf(userId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int balance = cursor.getInt(0);
            cursor.close();
            return balance;
        } else {
            if (cursor != null) {
                cursor.close();
            }
            return 0; // Default or handle error
        }
    }

    // get total number of transactions
    public int countTrans() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor allRows = db.query("Transactions", null, null, null, null, null, null);
        if (allRows != null) {
            return allRows.getCount();
        } else {
            return 0;
        }
    }


    // ------------------ Delete all for testing (except users) ----------------
    public void reset() {
        SQLiteDatabase db = this.getWritableDatabase();
        String deleteAllTransactions = "DELETE FROM Transactions";
        db.execSQL(deleteAllTransactions);
        String resetBalances = "UPDATE Users SET balance =" + 0;
        db.execSQL(resetBalances);
        String deleteAllSR = "DELETE FROM StudentRewards";
        db.execSQL(deleteAllSR);
        String deleteAllEvents = "DELETE FROM Events";
        db.execSQL(deleteAllEvents);
        String deleteAllEventsA = "DELETE FROM EventsA";
        db.execSQL(deleteAllEventsA);
        String deleteAllRewards = "DELETE FROM Rewards";
        db.execSQL(deleteAllRewards);
        String deleteAllRewardsA = "DELETE FROM RewardsA";
        db.execSQL(deleteAllRewardsA);
    }

    // Get all vendor approvals
    public Cursor getVendorApprovals() {
        SQLiteDatabase db1 = getWritableDatabase();
        return db1.query("VendorApproval", null, null, null, null, null, "name");
    }

    // Update vendor approval status
    public void updateVendorApprovalStatus(String username, int approved) {
        SQLiteDatabase db1 = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("approved", approved);
        db1.update("VendorApproval", cv, "username = ?", new String[] {username});
    }

    // Check if username exists in Users table
    public boolean checkUserExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("Users", 
            new String[]{"username"}, 
            "username = ?", 
            new String[]{username}, 
            null, null, null);
        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    // Check approval status of a username
    // Returns: 
    // -1: No approval record found (can register)
    // 0: Pending approval (cannot register)
    // 1: Approved (cannot register)
    // 2: Refused (can register)
    public int checkApprovalStatus(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("VendorApproval", 
            new String[]{"approved"}, 
            "username = ?", 
            new String[]{username}, 
            null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            int status = cursor.getInt(0);
            cursor.close();
            return status;
        }
        
        if (cursor != null) {
            cursor.close();
        }
        return -1; // No approval record found
    }

    // Check if username can be registered
    // Returns true if username can be registered (not in Users table and either no approval record or refused)
    public boolean canRegister(String username) {
        // First check if username exists in Users table
        if (checkUserExists(username)) {
            return false;
        }
        
        // Then check approval status
        int approvalStatus = checkApprovalStatus(username);
        // Can register if no approval record (-1) or refused (2)
        return approvalStatus == -1 || approvalStatus == 2;
    }

}
