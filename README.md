# photo_viewer_server
> This is the server for [photo_viewer](https://github.com/GITjest/photo_viewer).

## Setup
```
# Clone this repository
git clone https://github.com/GITjest/photo_viewer_server

# Using your IDE
```

## Message structure

| Request type  | Argument | Description | Response structure |
| ------------- | ------------- | ------------- | ------------- |
| GET | [catalog name] | Sends a list of categories | [true] [amount] ([categories] * amount) |
| GET_IMAGE | [catalog/photo name] | Sends photos | [true] [amount] ([name] [size] * amount) ([photo bits] * amount) |
| LOGIN | [login] [password] | Logs the user in | [true] [amount] ([categories] * amount) |
| CREATE | [category name] | Create category | [true] [amount] ([categories] * amount) |
| CREATE | [photo name] [photo size] [photo bits] | Create photo | [true] [1] [name] |
| DELETE | [catalog/photo name] | Removes category/photo | (catalog [true] [amount] ([categories] * amount)) (photo [true] [1] [category]) |
| UPDATE | [old catalog/photo name] [new catalog/photo name] | Update category/photo | (catalog [true] [amount] ([categories] * amount)) (photo [true] [1] [name]) |
| CREATE_ACCOUNT | [login] [password] | Creates an account | [true] [1] [login] |
| DELETE_ACCOUNT | [login] | Removes an account | [true] [1] [login] |
| UPDATE_ACCOUNT | [login] [new login] [password] [new password] | Update an account | [true] [1] [new login] |

If there is an error it will return [false] [[error](https://github.com/GITjest/photo_viewer_server/blob/master/src/Error.java)]
