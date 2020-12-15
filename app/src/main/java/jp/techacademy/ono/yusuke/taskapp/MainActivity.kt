package jp.techacademy.ono.yusuke.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*

const val  EXTRA_TASK = "jp.techacademy.ono.yusuke.taskapp.TASK"

class MainActivity : AppCompatActivity() {
    //TaskAdapterを保持するプロパティを定義する
    private lateinit var mRealm: Realm

    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            startActivity(intent)
        }

        //Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        //ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        //ListViewをタップしたときの処理
        listView1.setOnItemClickListener { parent, view, position, id ->
            //入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            //タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            //ダイアログを表示する
            val builder = AlertDialog.Builder(this@MainActivity)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK") { _, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()


                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this@MainActivity,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadListView()
            }

            builder.setNegativeButton("CANCELL", null)

            val dialog = builder.create()
            dialog.show()

            true

        }

        //onCreateメソッドでreloadListViewメソッドを呼び出す

        reloadListView()


    }

    private fun reloadListView() {
        //Realmデータベースから、「すべてのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults =
            mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

        //上記の結果をtaskListとしてセットすr
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

        //TaskもListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        //表示を更新するために、アダプターにデータが変更されたことを知らせる。
        mTaskAdapter.notifyDataSetChanged()

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {


                    val taskRealmResults =
                        mRealm.where(Task::class.java).equalTo("category", newText).findAll()
                            .sort("date", Sort.DESCENDING)

                    mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

                    listView1.adapter = mTaskAdapter

                    mTaskAdapter.notifyDataSetChanged()
                 if(newText == "") {
                    val taskRealmResults =
                        mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

                    mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

                    listView1.adapter = mTaskAdapter

                    mTaskAdapter.notifyDataSetChanged()

                }

                return false

            }
            override fun onQueryTextSubmit(query: String): Boolean {


                    val taskRealmResults =
                        mRealm.where(Task::class.java).equalTo("category", query).findAll()
                            .sort("date", Sort.DESCENDING)

                    mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

                    listView1.adapter = mTaskAdapter

                    mTaskAdapter.notifyDataSetChanged()
                if(query == ""){
                    val taskRealmResults =
                        mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

                    mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

                    listView1.adapter = mTaskAdapter

                    mTaskAdapter.notifyDataSetChanged()

                }

                return false
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }
}
