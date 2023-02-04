FROM registry.yandex.net/rtc-base/bionic:stable

RUN apt-get update \
    && apt-get install -y gcc-arm-linux-gnueabihf \
                          ssh \
                          unzip \
                          zip \
                          libglib2.0-dev \
                          libfdt-dev \
                          libpixman-1-dev \
                          zlib1g-dev \
                          flex \
                          bison \
                          ninja-build \
                          socat \
    && apt-get clean

RUN ln -s /usr/arm-linux-gnueabihf/lib/ld-linux-armhf.so.3 /lib/ld-linux-armhf.so.3 \
    && ln -s /usr/arm-linux-gnueabihf/lib/libdl.so.2 /lib/libdl.so.2 \
    && ln -s /usr/arm-linux-gnueabihf/lib/libpthread.so.0 /lib/libpthread.so.0 \
    && ln -s /usr/arm-linux-gnueabihf/lib/libc.so.6 /lib/libc.so.6


RUN mkdir build
WORKDIR /build

# Сборка QEMU
ENV QEMU="qemu-6.0.0"
RUN wget https://storage.yandex-team.ru/get-devtools/995452/d79b71dfcd3bbd92c2e2b8720d44927cb3bb68df/"$QEMU".tar.xz \
    && xz -d "$QEMU".tar.xz \
    && tar -xf "$QEMU".tar

RUN cd /build/$QEMU \
    && ./configure --target-list="arm-linux-user" \
    && make -j4 \
    && cd ../.. \
    && strip build/$QEMU/build/qemu-arm \
    && cp build/$QEMU/build/qemu-arm bin/ -v

# Настройка доступа по ssh
RUN printf '\nPermitRootLogin yes\n' >> /etc/ssh/sshd_config
RUN echo 'root:password' | chpasswd

COPY templates /kkt/templates
COPY setup_and_run_kkt.sh /kkt/setup_and_run_kkt.sh
COPY data /FR

COPY patch_fn_file/patch_fn_file.py /kkt/patch_fn_file.py

RUN chmod +x /FR/FR_* \
    && chmod +x /FR/start.sh \
    && chmod +x /kkt/setup_and_run_kkt.sh \
    && touch /FR/debug.flag

EXPOSE 4444
EXPOSE 3333
EXPOSE 22
