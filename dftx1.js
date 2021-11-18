
/**
 * @fileoverview Template to compose HTTP reqeuest.
 * 
 */

const url = `http://api.duofu.xqustar.com/api/goods/exchange`;
const method = `POST`;
const headers = {
'uuid' : `68E61D49A875224A0F7BFEDBDFC3112F`,
'Connection' : `close`,
'Accept-Encoding' : `br;q=1.0, gzip;q=0.9, deflate;q=0.8`,
'channel' : `App Store`,
'Content-Type' : `application/json`,
'User-Agent' : `INT/1.0.7 (com.zs.duofu; build:14; iOS 14.3.0) Alamofire/5.4.1`,
'platform' : `ios`,
'appName' : `duofu`,
'Authorization' : `eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJpbnQiLCJpZCI6IjIxMTEwNjE1OTA5MTAxMzk3NjA2NCIsInR5cGUiOiJ3ZWl4aW4ifQ.l5PSsZwH-sgrNPT9-BUCyYjwy2-iNvpWcDsr38NFkyU`,
'versionname' : `1.0.7`,
'Host' : `api.duofu.xqustar.com`,
'versioncode' : `14`,
'Cookie' : `JSESSIONID=68E61D49A875224A0F7BFEDBDFC3112F`,
'Accept-Language' : `zh-Hans-CN;q=1.0`,
'Accept' : `*/*`
};
const body = `{"productId":"23b537b1-a6e0-4f99-9f66-137e1534c0a9","sum":1,"addressID":""}`;

const myRequest = {
    url: url,
    method: method,
    headers: headers,
    body: body
};

$task.fetch(myRequest).then(response => {
    console.log(response.statusCode + "\n\n" + response.body);
    $done();
}, reason => {
    console.log(reason.error);
    $done();
});
