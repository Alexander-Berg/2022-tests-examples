<script type="text/javascript">
var demo = {
    catalog : {
        category : {
            getlist : {
                params : '',
                url : ''
            }
        },
        mark : {
            getlist : {
                params : 'category_id=15',
                url : ''
            }
        },
        folder : {
            getlist : {
                params : 'category_id=15&mark_id=30&folder_id=18133&level=2',
                url : '',
            }
        },
        modification : {
            getlist : {
                params : 'folder_id=4092',
                url : '',
            }
        }
    },
    all : {
        sale : {
            search : {
                params : 'category_id=15&section_id=1&limit=10',
                url : ''
            },
            get : {
                params : 'category_id=15&section_id=1&sale_id=12118397-76a05',
                url : ''
            },
            getphones : {
                params : 'category_id=15&section_id=1&sale_id=12118397-76a05',
                url : ''
            }
        },
    },
    news : {
        category : {
            getlist : {
                params : '',
                url : ''
            }
        },
        news : {
            getlist : {
                params : '',
                url : ''
            },
            get : {
                params : 'news_id=706',
                url : ''
            }
        },
        testdrive : {
            getlist : {
                params : '',
                url : ''
            },
            get : {
                params : 'testdrive_id=508',
                url : ''
            }
        }
    },
    my : {
        review : {
            getlist : {
                params : '',
                url : ''
            },
            get : {
                params : 'review_id=9971',
                url : ''
            }
        }
    },
    nomer : {
        category : {
            getlist : {
                params : '',
                url : ''
            }
        },
        story : {
            getlist  : {
                params : '',
                url : ''
            },
            getinfo : {
                params : 'id=605',
                url : ''
            },
            add : {
                params : '',
                url : ''
            }
        }
    },
    geobase : {
        country : {
            getlist : {
                params : '',
                url : ''
            }
        },
        region : {
            getlist : {
                params : 'country_id=1',
                url : ''
            }
        },
        city : {
            getlist : {
                params : 'region_id=47',
                url : ''
            },
            getnearest : {
                params : 'latitude=55.7558&longitude=37.6176',
                url : ''
            },
        }
    },
    comments : {
        comment : {
            getlist : {
                params : 'project=my5&object_id=9971&object=0',
                url : ''
            },
            add : {
                params : 'project=my5&object_id=9971&object=0&text=12',
                url : ''
            }
        }
    },
    users : {
        auth : {
            login : {
                params : 'login=alexandrov@auto.ru&pass=autoru',
                url : ''
            },
            logout : {
                params : '',
                url : ''
            },
            validsession : {
                params : '',
                url : ''
            },
            registration : {
                params : 'email=alexandrov@auto.ru&password=123456&confirm=123456',
                url : ''
            }
        },
        profile : {
            me : {
                params : '',
                url : ''
            }
        },
        message : {
            send : {
                params : 'to_user_id=2977812&subject=привет&text=медвед',
                url : ''
            }
        }
    }
}
</script>
<h1><?php echo $this->page_title; ?></h1>
<?php echo $this->form->getFormStartTag(); ?>
<fieldset>
    <label><strong>Доступ к API</strong></label>
    <?php echo $this->form->getElement('api_url', array('size' => 96)); ?>
    <?php echo $this->form->getElement('version'); ?>
    <br/>
    <?php echo $this->form->key_login->getLabel(); ?><br/><?php echo $this->form->getElement('key_login', array('size' => 96)); ?><br/>
    <?php echo $this->form->key_secret->getLabel(); ?><br/><?php echo $this->form->getElement('key_secret', array('size' => 96)); ?><br/>
    <?php echo $this->form->interface->getLabel(); ?><br/><?php echo $this->form->getElement('interface'); ?><br/>
    <?php echo $this->form->format->getLabel(); ?><br/><?php echo $this->form->getElement('format'); ?><br/>
</fieldset>
<fieldset>
    <label><strong>Метод API</strong></label>
    <br/>Проект / Контроллер / Метод<br/>
    <?php echo $this->form->getElement('project', array('style'=> 'min-width:20%;')); ?>
    <?php echo $this->form->getElement('controller', array('style'=> 'min-width:20%;')); ?>
    <?php echo $this->form->getElement('method', array('style'=> 'min-width:20%;')); ?>
    <br/>
    <?php echo $this->form->sid->getLabel(); ?><br/>&nbsp;<?php echo $this->form->getElement('sid', array('size' => 96)); ?>
    <br/>
    <?php echo $this->form->params->getLabel(); ?><br/><strong>?</strong><?php echo $this->form->getElement('params', array('size' => 95)); ?>
</fieldset>
<fieldset>
    <?php echo $this->form->getElement('send'); ?>
</fieldset>
<?php echo $this->form->getFormEndTag(); ?>
<pre></pre>
