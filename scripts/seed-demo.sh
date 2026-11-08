#!/bin/sh
set -eu

USER_API=${USER_API:-http://user-service:8081}
POST_API=${POST_API:-http://post-service:8082}

user_id() {
  printf '00000000-0000-0000-0000-%012d' "$1"
}

token() {
  curl -sf -X POST "$USER_API/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"provider\":\"LOCAL\",\"username\":\"test$1\",\"password\":\"test\"}" \
    | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'
}

follow() {
  curl -sf -o /dev/null -X POST "$USER_API/api/users/$(user_id "$2")/follow" \
    -H "Authorization: Bearer $(token "$1")"
}

post() {
  curl -sf -o /dev/null -X POST "$POST_API/api/posts" \
    -H "Authorization: Bearer $(token "$1")" \
    -H 'Content-Type: application/json' \
    -d "{\"content\":\"Test user $1: message $2\"}"
}

if curl -sf "$USER_API/api/users/$(user_id 2)" -H "Authorization: Bearer $(token 1)" | grep -q '"isFollowing":true'; then
  echo "Demo data already present"
  exit 0
fi

for target in 2 3 4; do follow 1 "$target"; done
for follower in 2 3 4; do follow "$follower" 1; done
follow 3 4
follow 4 3

for message in 1 2; do
  for author in 1 2 3 4; do post "$author" "$message"; done
done
post 1 3
post 3 3

echo "Demo data created"
