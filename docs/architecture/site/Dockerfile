# The static LikeC4 site, baked into nginx-unprivileged.
#
# The build happens on the CI runner, not here: out/ is the output of likec4 build, copied in as it is. The web
# root is /srv/site so the stock error page of the base image is not served beside the site. The image runs as
# user 101 on port 8080 and its default command keeps nginx in the foreground.
FROM nginxinc/nginx-unprivileged:1.30.6-alpine-slim@sha256:5d8bb9437d74a600ec5dbf90c035451c724dc33eac8d3a71078f6f8612c345f6
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY out/ /srv/site/
