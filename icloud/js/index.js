 var leftlist=document.querySelector(".left_list");
    var left=document.querySelector(".left").offsetHeight;
    var lefttop=document.querySelector(".left_top").offsetHeight;
    leftlist.style.height=(left-lefttop)+"px";
    var inputs=document.querySelector(".right_input>input");
    var rightinput=document.querySelector(".right_input");
    inputs.onfocus=function(){
         rightinput.style.background="#fff"
    }
    inputs.onblur =function(){
        rightinput.style.background=""
    }
var colors=['#0F95D6','#1243AF','#12AF8E','#93AF12','#27AF12','#AF12A5','#954D24']
    var arr=[
        {
            id:1,
            title:'列表1',
            todos:[
                {con:"礼拜一开会",done:true},
                {con:"礼拜一开会",done:true},
                {con:"0爱你ascsa",done:false},
                {con:"3带你ascsa",done:true},
                {con:"3带你ascsa",done:true},
            ],
            color:colors[0],
        },
        {
            id:2,
            title:'列表2',
            todos:[
                {con:"22带你ascsa",done:true},
                {con:"32带你ascsa",done:true},
                {con:"0打你ascsa",done:false},
                {con:"3带你ascsa",done:true},
                {con:"3带你ascsa",done:true},

            ],
            color:colors[1],
        },
        {
            id:3,
            title:'列表3',
            todos:[
                {con:"1:00吃饭",done:false},
                {con:"2:00开会",done:false},
                {con:"3:00聊天",done:true},
                {con:"4:00上班",done:true},
                {con:"5:00下班",done:false},
                {con:"6:00晚饭",done:false},

            ],
            color:colors[2],
        },
    ]
    var app=angular.module('app',[]);
    app.controller('c1',function($scope,$filter){
        $scope.arr=arr;
        $scope.index=arr.length-1;
        $scope.add=function(i,c){
            $scope.index=i-1;
            $scope.cl=c;
            $scope.tagle=false;
            geidonenum()
        }
        $scope.addele=function(i){
           var id=$scope.arr.length+1;
             $scope.tagle=false;
            $scope.arr.push({
                id:id,
                title:'列表'+id,
                todos:[],
                color:colors[(arr.length)%7],
            })
            $scope.index=$scope.arr.length-1;
             geidonenum()
        }
        // 开关
        $scope.tagle=false;
        $scope.tag=function(){
            $scope.tagle=!$scope.tagle;
        }
        //已经完成
        $scope.done_num=0;
        geidonenum()
        function geidonenum(){
           var bb=$scope.arr[$scope.index].todos
            var bbb=bb.filter(function(v,i){
               if (v.done) {
                   return true;
               }else{
                   return false;
               }
           })
             $scope.done_num=bbb.length
        }
        //清除已经完成的
         $scope.delall=function(){
             var odd=$scope.arr[$scope.index].todos;
             $scope.arr[$scope.index].todos=$filter('filter')(odd,function(v){
                  if(!v.done){
                    return true;
                  }
             })   
                geidonenum()
         }
        //点击的改变
        $scope.chargestate=function(o,fl){
            o.done=fl;
            geidonenum()
        }
      
        //添加新的项目
        $scope.addnewpro=function(){
             var odd=$scope.arr[$scope.index].todos;
             odd.push({
                con:"",
                done:false,
             })
        }
        $scope.isshow=false;

        $scope.show=function(v){
        $scope.isshow=!$scope.isshow;
         $scope.showTitle=arr[$scope.index].title
         $scope.showColor=arr[$scope.index].color
        }
        $scope.selectColor=function(v){
            $scope.showColor=v
        }
        $scope.newall=function(){
            $scope.arr[$scope.index].title=$scope.showTitle
            $scope.arr[$scope.index].color=$scope.showColor
            $scope.isshow=false;

        }
        // 删除
        $scope.delsome=function(v){
            $scope.isshow=false; 
             $scope.arr.splice(v,1);
             $scope.index=$scope.arr.length-1;


        }
        $scope.ooo=function(){
           $scope.isshow=false; 
        }
        // 颜色的遍历
        $scope.colorall=colors;
         
    })


