# 大文件上传
## 上传例子
### socket上传例子
```
TaskUtil.getImpl().runTask(new Runnable() {
            @Override
            public void run() {
                SocketBigFileUpload.Builder builder = new SocketBigFileUpload.Builder();
                builder.setFile(new File(Environment.getExternalStorageDirectory() + "/360/test.mp4")).setContext
                        (MainActivity.this).setListener(new IBigFileUpload.BigFileUploadListener() {
                    @Override
                    public void success(IBigFileUpload.ResonseInfo<String> info) {
                        LogUtil.msg("------->成功");
                    }

                    @Override
                    public void failure(IBigFileUpload.ResonseInfo<String> info) {
                        LogUtil.msg("------->失败");
                    }

                    @Override
                    public void process(float percent) {

                    }
                }).build("192.168.80.110", 2347).upload();
            }
        });
```
