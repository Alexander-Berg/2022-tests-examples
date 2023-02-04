#!/usr/bin/env bash
systemctl stop hue.service;
sleep 5;
systemctl start hue.service;
sleep 10;
monrun -r hue
