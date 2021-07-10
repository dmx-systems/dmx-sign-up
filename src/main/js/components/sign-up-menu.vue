<template>
    <div class="dmx-sign-up">
        <el-button size="mini" v-if="!isLoggedIn && showSignupButton">
            <el-link href="/sign-up" :underline="false">Sign up</el-link>
        </el-button>
    </div>
</template>

<script>
export default {

  inject: {
    http: 'axios'
  },

  created: function () {
    this.checkSelfRegistrationActive()
  },

  data() {
    return {
      showSignupButton: false
    }
  },

  computed: {
    isLoggedIn () {
      return (this.$store.state.accesscontrol.username)
    }
  },

  methods: {
    checkSelfRegistrationActive: function() {
      this.http.get(`/sign-up/self-registration-active`).then(response => {
        this.showSignupButton = (response.data == true)
      })
    }
  }

}
</script>

<style>
.dmx-sign-up button {
    margin-left: .75em;
    /** To align with Login State Menu: Padding and Font-Size copied from .dm5-login-state .el-button.sign-in **/
    font-size: 12px !important;
    padding: 4px 8px !important;
    position: relative;
}
.dmx-sign-up button .el-link {
    font-size: 12px;
}
</style>
